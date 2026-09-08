package com.cenlottery.service;

import com.cenlottery.domain.Round;
import com.cenlottery.domain.Ticket;
import com.cenlottery.util.Constants;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Implements requirement sections 4-7: winning tier payouts, prize pool composition,
 * the 1st-3rd place proportional distribution, carryover accounting, and the
 * negative-pool exception (equal payout) path.
 *
 * Money is kept as BigDecimal throughout so that truncation only happens at the exact
 * points the spec calls for (per-ticket final payout), never in intermediate steps.
 */
@Service
public class SettlementService {

    public void settle(Round round, List<Ticket> tickets, BigDecimal carryInPool, DrawService drawService) {
        settleWithDraw(round, tickets, carryInPool, drawService.drawGeneralBalls(), drawService.drawPowerball(), drawService);
    }

    /**
     * Core settlement logic with the drawn numbers passed in explicitly, so it can be exercised
     * deterministically from tests without depending on SecureRandom. The production path
     * ({@link #settle}) just draws fresh numbers and delegates here.
     */
    public void settleWithDraw(Round round, List<Ticket> tickets, BigDecimal carryInPool,
                                List<Integer> drawnGeneral, int drawnPowerball, DrawService drawService) {
        round.setCarryInPool(carryInPool);
        round.setDrawnGeneralBalls(drawnGeneral);
        round.setDrawnPowerball(drawnPowerball);

        int tier1 = 0, tier2 = 0, tier3 = 0;
        int tier4Normal = 0, tier4PowerUp = 0, tier5Normal = 0, tier5PowerUp = 0, noWin = 0;

        for (Ticket t : tickets) {
            int matches = drawService.countMatches(t.getGeneralBalls(), drawnGeneral);
            boolean powerballMatch = t.getPowerball() == drawnPowerball;
            Integer tier = drawService.tierFor(matches, powerballMatch);
            t.setTier(tier);
            if (tier == null) {
                noWin++;
            } else {
                switch (tier) {
                    case 1: tier1++; break;
                    case 2: tier2++; break;
                    case 3: tier3++; break;
                    case 4: if (t.isPowerUp()) tier4PowerUp++; else tier4Normal++; break;
                    case 5: if (t.isPowerUp()) tier5PowerUp++; else tier5Normal++; break;
                    default: break;
                }
            }
        }

        BigDecimal basicGameSales = Constants.NORMAL_PRICE.multiply(BigDecimal.valueOf(tickets.size()));
        BigDecimal currentPool = carryInPool.add(basicGameSales.multiply(Constants.POOL_SALES_RATIO));

        BigDecimal fixedPrizeTotal = Constants.FIXED_4TH.multiply(BigDecimal.valueOf(tier4Normal))
                .add(Constants.FIXED_4TH_POWERUP.multiply(BigDecimal.valueOf(tier4PowerUp)))
                .add(Constants.FIXED_5TH.multiply(BigDecimal.valueOf(tier5Normal)))
                .add(Constants.FIXED_5TH_POWERUP.multiply(BigDecimal.valueOf(tier5PowerUp)));

        BigDecimal remainingPool = currentPool.subtract(fixedPrizeTotal);

        round.setBasicGameSales(basicGameSales);
        round.setCurrentPool(currentPool);
        round.setFixedPrizeTotal(fixedPrizeTotal);
        round.setRemainingPool(remainingPool);
        round.setTier1WinningTickets(tier1);
        round.setTier2WinningTickets(tier2);
        round.setTier3WinningTickets(tier3);
        round.setTier4WinningTickets(tier4Normal + tier4PowerUp);
        round.setTier5WinningTickets(tier5Normal + tier5PowerUp);
        round.setNoWinTickets(noWin);

        BigDecimal totalPaidOut;

        if (remainingPool.signum() >= 0) {
            round.setNegativePoolException(false);

            BigDecimal tier1Alloc = remainingPool.multiply(Constants.TIER1_RATIO);
            BigDecimal tier2Alloc = remainingPool.multiply(Constants.TIER2_RATIO);
            BigDecimal tier3Alloc = remainingPool.multiply(Constants.TIER3_RATIO);
            round.setTier1Alloc(tier1Alloc);
            round.setTier2Alloc(tier2Alloc);
            round.setTier3Alloc(tier3Alloc);

            BigDecimal tier1Per = perTicket(tier1Alloc, tier1);
            BigDecimal tier2Per = perTicket(tier2Alloc, tier2);
            BigDecimal tier3Per = perTicket(tier3Alloc, tier3);
            round.setTier1PayoutPerTicket(tier1Per);
            round.setTier2PayoutPerTicket(tier2Per);
            round.setTier3PayoutPerTicket(tier3Per);

            BigDecimal paidTier1 = tier1Per == null ? BigDecimal.ZERO : tier1Per.multiply(BigDecimal.valueOf(tier1));
            BigDecimal paidTier2 = tier2Per == null ? BigDecimal.ZERO : tier2Per.multiply(BigDecimal.valueOf(tier2));
            BigDecimal paidTier3 = tier3Per == null ? BigDecimal.ZERO : tier3Per.multiply(BigDecimal.valueOf(tier3));

            for (Ticket t : tickets) {
                t.setSettlementType("NORMAL");
                Integer tier = t.getTier();
                if (tier == null) {
                    t.setPayout(BigDecimal.ZERO);
                } else if (tier == 1) {
                    t.setPayout(tier1Per);
                } else if (tier == 2) {
                    t.setPayout(tier2Per);
                } else if (tier == 3) {
                    t.setPayout(tier3Per);
                } else if (tier == 4) {
                    t.setPayout(t.isPowerUp() ? Constants.FIXED_4TH_POWERUP : Constants.FIXED_4TH);
                } else if (tier == 5) {
                    t.setPayout(t.isPowerUp() ? Constants.FIXED_5TH_POWERUP : Constants.FIXED_5TH);
                }
                t.setStatus(Ticket.SETTLED);
            }

            totalPaidOut = paidTier1.add(paidTier2).add(paidTier3).add(fixedPrizeTotal);
        } else {
            round.setNegativePoolException(true);
            int totalWinning = tier1 + tier2 + tier3 + tier4Normal + tier4PowerUp + tier5Normal + tier5PowerUp;
            BigDecimal equalPer;
            if (totalWinning == 0 || currentPool.signum() <= 0) {
                // currentPool should never be negative in real operation (it's carry-in plus a
                // non-negative sales share); this guard only protects against an operator feeding
                // an unrealistic test override into /api/admin/rounds/current/pool-override.
                equalPer = BigDecimal.ZERO;
            } else {
                equalPer = currentPool.divide(BigDecimal.valueOf(totalWinning), 0, RoundingMode.DOWN);
            }
            round.setEqualPayoutPerTicket(equalPer);

            for (Ticket t : tickets) {
                if (t.getTier() == null) {
                    t.setPayout(BigDecimal.ZERO);
                    t.setSettlementType("NORMAL");
                } else {
                    t.setPayout(equalPer);
                    t.setSettlementType("EQUAL");
                }
                t.setStatus(Ticket.SETTLED);
            }
            totalPaidOut = equalPer.multiply(BigDecimal.valueOf(totalWinning));
        }

        round.setTotalPaidOut(totalPaidOut);
        round.setCarryOutPool(currentPool.subtract(totalPaidOut));
        round.setStatus(Round.SETTLED);
        round.setSettledAt(System.currentTimeMillis());
    }

    private BigDecimal perTicket(BigDecimal alloc, int winnerCount) {
        if (winnerCount <= 0) return null;
        return alloc.divide(BigDecimal.valueOf(winnerCount), 0, RoundingMode.DOWN);
    }
}
