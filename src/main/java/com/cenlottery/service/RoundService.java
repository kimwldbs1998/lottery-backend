package com.cenlottery.service;

import com.cenlottery.domain.Round;
import com.cenlottery.domain.Ticket;
import com.cenlottery.repository.RoundRepository;
import com.cenlottery.repository.TicketRepository;
import com.cenlottery.util.Constants;
import jakarta.annotation.PostConstruct;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

/**
 * Owns round (회차) lifecycle: opening a round, running the 5-minute sale window,
 * closing sales, running the draw + settlement, and opening the next round with the
 * carried-over pool. Requirement section 3.
 *
 * Built for a single backend instance/process (same assumption as the original build) - the lock
 * below is a plain in-JVM lock, not a distributed one.
 */
@Service
public class RoundService {
    private static final Logger LOG = Logger.getLogger("RoundService");

    private final RoundRepository roundRepository;
    private final TicketRepository ticketRepository;
    private final DrawService drawService;
    private final SettlementService settlementService;

    /** All round-affecting operations (rollover, force-settle, pool override, purchase-confirm's round check) hold this. */
    private final ReentrantLock roundLock = new ReentrantLock();

    /**
     * Exposed as methods (not the raw field) so external callers always go through the
     * proxy and reach the real target instance - direct field access on the CGLIB proxy
     * that Spring creates for this @Transactional-bearing class reads an uninitialized
     * field and NPEs, since proxies are instantiated without running field initializers.
     */
    public void lockRound() {
        roundLock.lock();
    }

    public void unlockRound() {
        roundLock.unlock();
    }

    public RoundService(RoundRepository roundRepository, TicketRepository ticketRepository,
                         DrawService drawService, SettlementService settlementService) {
        this.roundRepository = roundRepository;
        this.ticketRepository = ticketRepository;
        this.drawService = drawService;
        this.settlementService = settlementService;
    }

    @PostConstruct
    public void start() {
        roundLock.lock();
        try {
            Round current = roundRepository.findFirstByStatusOrderByRoundNumberDesc(Round.OPEN).orElse(null);
            if (current == null) {
                openRound(1L, BigDecimal.ZERO);
            } else {
                catchUpIfNeeded();
            }
        } finally {
            roundLock.unlock();
        }
        LOG.info("RoundService started. Current round: " + getCurrentRound().getRoundNumber());
    }

    public Round getCurrentRound() {
        return roundRepository.findFirstByStatusOrderByRoundNumberDesc(Round.OPEN).orElse(null);
    }

    public Round getRound(long roundNumber) {
        return roundRepository.findById(roundNumber).orElse(null);
    }

    @Transactional
    protected void openRound(long roundNumber, BigDecimal carryIn) {
        Round r = new Round();
        r.setRoundNumber(roundNumber);
        r.setStatus(Round.OPEN);
        r.setStartTime(System.currentTimeMillis());
        r.setEndTime(r.getStartTime() + Constants.ROUND_DURATION_MS);
        r.setCarryInPool(carryIn);
        roundRepository.save(r);
    }

    private void catchUpIfNeeded() {
        // If the server was stopped past a round's close time, settle it (and any further
        // elapsed rounds) on startup so state isn't stuck showing a stale round.
        while (true) {
            Round current = getCurrentRound();
            if (current == null || !Round.OPEN.equals(current.getStatus())) break;
            if (System.currentTimeMillis() < current.getEndTime()) break;
            settleAndAdvance(current);
        }
    }

    /** Checked every second; cheap, and keeps the on-screen countdown / rollover accurate. */
    @Scheduled(fixedRate = 1000)
    public void rolloverIfNeeded() {
        roundLock.lock();
        try {
            Round current = getCurrentRound();
            if (current != null && System.currentTimeMillis() >= current.getEndTime()) {
                settleAndAdvance(current);
            }
        } catch (Exception e) {
            LOG.severe("Round rollover failed: " + e);
        } finally {
            roundLock.unlock();
        }
    }

    /** Settles the given (already-closed-by-time) round and opens the next one. Caller must hold roundLock. */
    @Transactional
    protected void settleAndAdvance(Round current) {
        List<Ticket> tickets = ticketRepository.findByRoundNumber(current.getRoundNumber());
        BigDecimal carryIn = current.getPoolOverride() != null ? current.getPoolOverride() : current.getCarryInPool();
        settlementService.settle(current, tickets, carryIn, drawService);
        ticketRepository.saveAll(tickets);
        roundRepository.save(current);
        openRound(current.getRoundNumber() + 1, current.getCarryOutPool());
        LOG.info("Round " + current.getRoundNumber() + " settled. Carryover to round "
                + (current.getRoundNumber() + 1) + ": " + current.getCarryOutPool());
    }

    /** Test/QA utility: force the current round to settle immediately instead of waiting out the 5 minutes. */
    public Round forceSettleCurrentRound() {
        roundLock.lock();
        try {
            Round current = getCurrentRound();
            if (current == null) {
                throw new IllegalStateException("현재 정산 가능한 진행중 회차가 없습니다.");
            }
            long settledRoundNumber = current.getRoundNumber();
            settleAndAdvance(current);
            return roundRepository.findById(settledRoundNumber).orElseThrow();
        } finally {
            roundLock.unlock();
        }
    }

    /**
     * Test/QA utility (requirement section 7 note): override the pool amount used when the
     * CURRENT round settles, so the negative-pool exception path can be reproduced and verified
     * without needing an unrealistic number of real purchases.
     */
    @Transactional
    public Round overrideCurrentRoundPool(BigDecimal amount) {
        roundLock.lock();
        try {
            Round current = getCurrentRound();
            if (current == null) {
                throw new IllegalStateException("현재 진행중인 회차가 없습니다.");
            }
            current.setPoolOverride(amount);
            return roundRepository.save(current);
        } finally {
            roundLock.unlock();
        }
    }
}
