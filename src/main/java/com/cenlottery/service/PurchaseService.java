package com.cenlottery.service;

import com.cenlottery.domain.Purchase;
import com.cenlottery.domain.Round;
import com.cenlottery.domain.Ticket;
import com.cenlottery.domain.User;
import com.cenlottery.exception.ApiException;
import com.cenlottery.repository.PurchaseRepository;
import com.cenlottery.repository.TicketRepository;
import com.cenlottery.repository.UserRepository;
import com.cenlottery.util.Constants;
import com.cenlottery.util.IdGenerator;
import com.cenlottery.util.MoneyFormat;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

/** Requirement sections 2, 9, 10, 11, 15: number/PowerUp validation, pricing, and the purchase transaction. */
@Service
public class PurchaseService {

    private final UserRepository userRepository;
    private final PurchaseRepository purchaseRepository;
    private final TicketRepository ticketRepository;
    private final RoundService roundService;

    public PurchaseService(UserRepository userRepository, PurchaseRepository purchaseRepository,
                            TicketRepository ticketRepository, RoundService roundService) {
        this.userRepository = userRepository;
        this.purchaseRepository = purchaseRepository;
        this.ticketRepository = ticketRepository;
        this.roundService = roundService;
    }

    public static class Selection {
        public List<Integer> generalBalls;
        public List<Integer> powerballs; // already expanded (ALL -> 0..9), de-duplicated, sorted
        public boolean powerUp;
        public boolean allSelected;
    }

    public Selection validate(List<Integer> generalBalls, List<Integer> rawPowerballs, Boolean allFlag, Boolean powerUp) {
        if (generalBalls == null || generalBalls.size() != Constants.GENERAL_BALL_PICK) {
            throw ApiException.badRequest("GENERAL_BALLS_COUNT_INVALID", "일반볼을 정확히 5개 선택해 주세요.");
        }
        Set<Integer> distinct = new LinkedHashSet<>(generalBalls);
        if (distinct.size() != Constants.GENERAL_BALL_PICK) {
            throw ApiException.badRequest("GENERAL_BALLS_INVALID", "일반볼은 서로 달라야 합니다.");
        }
        for (int n : distinct) {
            if (n < Constants.GENERAL_BALL_MIN || n > Constants.GENERAL_BALL_MAX) {
                throw ApiException.badRequest("GENERAL_BALLS_INVALID", "일반볼은 1~7 범위에서 선택해 주세요.");
            }
        }

        List<Integer> powerballs;
        if (Boolean.TRUE.equals(allFlag)) {
            powerballs = new ArrayList<>();
            for (int i = Constants.POWERBALL_MIN; i <= Constants.POWERBALL_MAX; i++) powerballs.add(i);
        } else {
            if (rawPowerballs == null || rawPowerballs.isEmpty()) {
                throw ApiException.badRequest("POWERBALL_REQUIRED", "파워볼을 한 개 이상 선택해 주세요.");
            }
            Set<Integer> pset = new LinkedHashSet<>(rawPowerballs);
            for (int n : pset) {
                if (n < Constants.POWERBALL_MIN || n > Constants.POWERBALL_MAX) {
                    throw ApiException.badRequest("POWERBALL_RANGE_INVALID", "파워볼은 0~9 범위에서 선택해 주세요.");
                }
            }
            powerballs = new ArrayList<>(pset);
            Collections.sort(powerballs);
        }

        if (powerballs.size() > Constants.MAX_TICKETS_PER_PURCHASE) {
            throw ApiException.badRequest("QUANTITY_EXCEEDED", "한 번에 구매할 수 있는 최대 수량(10매)을 초과했습니다.");
        }

        Selection sel = new Selection();
        sel.generalBalls = new ArrayList<>(distinct);
        Collections.sort(sel.generalBalls);
        sel.powerballs = powerballs;
        sel.powerUp = Boolean.TRUE.equals(powerUp);
        sel.allSelected = powerballs.size() == Constants.POWERBALL_COUNT;
        return sel;
    }

    public Map<String, Object> quote(String userId, Selection sel) {
        User user = userRepository.findById(userId).orElseThrow(() ->
                ApiException.unauthorized("SESSION_EXPIRED", "로그인이 필요합니다."));
        BigDecimal pricePerGame = sel.powerUp ? Constants.POWERUP_PRICE : Constants.NORMAL_PRICE;
        int gameCount = sel.powerballs.size();
        BigDecimal total = pricePerGame.multiply(BigDecimal.valueOf(gameCount));
        Round round = roundService.getCurrentRound();

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("roundNumber", round.getRoundNumber());
        m.put("generalBalls", sel.generalBalls);
        m.put("powerballs", sel.powerballs);
        m.put("powerUp", sel.powerUp);
        m.put("allSelected", sel.allSelected);
        m.put("gameCount", gameCount);
        m.put("pricePerGame", MoneyFormat.intStr(pricePerGame));
        m.put("totalAmount", MoneyFormat.intStr(total));
        m.put("currentBalance", MoneyFormat.intStr(user.getBalance()));
        BigDecimal projected = user.getBalance().subtract(total);
        m.put("projectedBalance", MoneyFormat.intStr(projected));
        m.put("sufficientBalance", projected.signum() >= 0);
        m.put("saleEndsAt", round.getEndTime());
        return m;
    }

    @Transactional
    public Purchase confirm(String userId, long requestedRoundNumber, Selection sel, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            idempotencyKey = IdGenerator.next("idem");
        }
        String finalKey = idempotencyKey;

        roundService.lockRound();
        try {
            Purchase existing = purchaseRepository.findByIdempotencyKey(finalKey).orElse(null);
            if (existing != null) {
                return existing;
            }

            Round round = roundService.getCurrentRound();
            if (round == null || round.getRoundNumber() != requestedRoundNumber
                    || System.currentTimeMillis() >= round.getEndTime()) {
                throw ApiException.conflict("ROUND_CHANGED", "회차가 변경되었습니다. 번호 선택 화면에서 다시 확인해 주세요.");
            }

            User user = userRepository.findById(userId).orElseThrow(() ->
                    ApiException.unauthorized("SESSION_EXPIRED", "로그인이 필요합니다."));

            BigDecimal pricePerGame = sel.powerUp ? Constants.POWERUP_PRICE : Constants.NORMAL_PRICE;
            int gameCount = sel.powerballs.size();
            BigDecimal total = pricePerGame.multiply(BigDecimal.valueOf(gameCount));

            if (user.getBalance().compareTo(total) < 0) {
                BigDecimal shortfall = total.subtract(user.getBalance());
                throw ApiException.badRequest("INSUFFICIENT_BALANCE",
                        "사용 가능 금액이 부족합니다. 필요 금액 Rs " + MoneyFormat.formatThousands(total)
                                + ", 부족한 금액 Rs " + MoneyFormat.formatThousands(shortfall) + "입니다.");
            }

            BigDecimal balanceBefore = user.getBalance();
            user.setBalance(user.getBalance().subtract(total));
            userRepository.save(user);

            Purchase purchase = new Purchase();
            purchase.setId(IdGenerator.next("purchase"));
            purchase.setUserId(userId);
            purchase.setRoundNumber(round.getRoundNumber());
            purchase.setGeneralBalls(sel.generalBalls);
            purchase.setPowerballs(sel.powerballs);
            purchase.setPowerUp(sel.powerUp);
            purchase.setAllSelected(sel.allSelected);
            purchase.setPricePerGame(pricePerGame);
            purchase.setGameCount(gameCount);
            purchase.setTotalAmount(total);
            purchase.setBalanceBefore(balanceBefore);
            purchase.setBalanceAfter(user.getBalance());
            purchase.setIdempotencyKey(finalKey);
            purchase.setCreatedAt(System.currentTimeMillis());
            purchase.setTicketIds(new ArrayList<>());

            for (int pb : sel.powerballs) {
                Ticket t = new Ticket();
                t.setId(IdGenerator.next("ticket"));
                t.setPurchaseId(purchase.getId());
                t.setUserId(userId);
                t.setRoundNumber(round.getRoundNumber());
                t.setGeneralBalls(sel.generalBalls);
                t.setPowerball(pb);
                t.setPowerUp(sel.powerUp);
                t.setPricePerGame(pricePerGame);
                t.setCreatedAt(purchase.getCreatedAt());
                ticketRepository.save(t);
                purchase.getTicketIds().add(t.getId());
            }

            return purchaseRepository.save(purchase);
        } finally {
            roundService.unlockRound();
        }
    }
}
