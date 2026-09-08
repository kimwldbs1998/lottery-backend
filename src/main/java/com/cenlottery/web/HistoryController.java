package com.cenlottery.web;

import com.cenlottery.domain.Purchase;
import com.cenlottery.domain.Round;
import com.cenlottery.domain.Ticket;
import com.cenlottery.repository.PurchaseRepository;
import com.cenlottery.repository.RoundRepository;
import com.cenlottery.repository.TicketRepository;
import com.cenlottery.security.CurrentUser;
import com.cenlottery.util.MoneyFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.*;

/** Requirement section 13: full purchase history + draw results, across every round the user has played. */
@RestController
@RequestMapping("/api/history")
public class HistoryController {
    private final PurchaseRepository purchaseRepository;
    private final RoundRepository roundRepository;
    private final TicketRepository ticketRepository;
    private final CurrentUser currentUser;

    public HistoryController(PurchaseRepository purchaseRepository, RoundRepository roundRepository,
                              TicketRepository ticketRepository, CurrentUser currentUser) {
        this.purchaseRepository = purchaseRepository;
        this.roundRepository = roundRepository;
        this.ticketRepository = ticketRepository;
        this.currentUser = currentUser;
    }

    @GetMapping
    public Map<String, Object> history(@RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "20") int size,
                                        @RequestParam(required = false) Long roundNumber,
                                        @RequestParam(required = false) String status) {
        String userId = currentUser.requireId();
        List<Purchase> all = purchaseRepository.findByUserIdOrderByCreatedAtDesc(userId);
        List<Map<String, Object>> mapped = new ArrayList<>();
        for (Purchase p : all) {
            Round round = roundRepository.findById(p.getRoundNumber()).orElse(null);
            if (roundNumber != null && p.getRoundNumber() != roundNumber) continue;
            if (status != null && round != null && !status.equalsIgnoreCase(round.getStatus())) continue;
            mapped.add(toHistoryEntry(p, round));
        }

        int from = Math.min(page * size, mapped.size());
        int to = Math.min(from + size, mapped.size());
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("items", mapped.subList(from, to));
        m.put("total", mapped.size());
        m.put("page", page);
        m.put("size", size);
        return m;
    }

    private Map<String, Object> toHistoryEntry(Purchase p, Round round) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("purchaseId", p.getId());
        m.put("transactionId", p.getId());
        m.put("roundNumber", p.getRoundNumber());
        m.put("purchaseDateTime", p.getCreatedAt());
        m.put("drawDateTime", round == null ? null : round.getEndTime());
        m.put("roundStatus", round == null ? "UNKNOWN" : round.getStatus());
        m.put("generalBalls", p.getGeneralBalls());
        m.put("powerUp", p.isPowerUp());
        m.put("allSelected", p.isAllSelected());
        m.put("pricePerGame", MoneyFormat.intStr(p.getPricePerGame()));
        m.put("gameCount", p.getGameCount());
        m.put("totalAmount", MoneyFormat.intStr(p.getTotalAmount()));

        List<Map<String, Object>> games = new ArrayList<>();
        BigDecimal totalPayout = BigDecimal.ZERO;
        boolean anySettled = false;
        boolean anyEqual = false;
        for (String ticketId : p.getTicketIds()) {
            Ticket t = ticketRepository.findById(ticketId).orElse(null);
            if (t == null) continue;
            Map<String, Object> g = new LinkedHashMap<>();
            g.put("ticketId", t.getId());
            g.put("powerball", t.getPowerball());
            g.put("status", t.getStatus());
            g.put("tier", t.getTier());
            g.put("tierLabel", tierLabel(t.getTier(), t.getStatus()));
            g.put("settlementType", t.getSettlementType());
            if (t.getPayout() != null) {
                g.put("payout", MoneyFormat.intStr(t.getPayout()));
                totalPayout = totalPayout.add(t.getPayout());
                anySettled = true;
            }
            if ("EQUAL".equals(t.getSettlementType())) anyEqual = true;
            games.add(g);
        }
        m.put("games", games);
        m.put("totalPayout", anySettled ? MoneyFormat.intStr(totalPayout) : null);
        m.put("equalPayoutApplied", anyEqual);
        return m;
    }

    private String tierLabel(Integer tier, String status) {
        if (!Ticket.SETTLED.equals(status)) return "추첨 대기";
        if (tier == null) return "미당첨";
        return tier + "등";
    }
}
