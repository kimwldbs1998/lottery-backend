package com.cenlottery.domain;

import com.cenlottery.util.MoneyFormat;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "rounds")
@Getter
@Setter
@NoArgsConstructor
public class Round {
    public static final String OPEN = "OPEN";
    public static final String SETTLED = "SETTLED";

    @Id
    private Long roundNumber;

    @Column(nullable = false)
    private String status = OPEN;

    private long startTime;
    private long endTime; // sale close time (startTime + 5 minutes)

    /** Carryover pool inherited from the previous round (before this round's sales are added). */
    @Column(nullable = false, precision = 20, scale = 6)
    private BigDecimal carryInPool = BigDecimal.ZERO;

    /** Set only by the admin test endpoint to reproduce the negative-pool exception scenario. */
    @Column(precision = 20, scale = 6)
    private BigDecimal poolOverride;

    // ----- filled in at settlement time -----
    @ElementCollection(fetch = jakarta.persistence.FetchType.EAGER)
    @CollectionTable(name = "round_drawn_general_balls", joinColumns = @jakarta.persistence.JoinColumn(name = "round_number"))
    @OrderColumn(name = "idx")
    @Column(name = "ball")
    private List<Integer> drawnGeneralBalls;

    private Integer drawnPowerball;

    @Column(precision = 20, scale = 6)
    private BigDecimal basicGameSales;      // total normal-priced sales (game count * 40), PowerUp surcharge excluded
    @Column(precision = 20, scale = 6)
    private BigDecimal currentPool;         // carryInPool (or override) + basicGameSales * 50%
    @Column(precision = 20, scale = 6)
    private BigDecimal fixedPrizeTotal;     // 4th/5th place actual payout total (incl. PowerUp x2)
    @Column(precision = 20, scale = 6)
    private BigDecimal remainingPool;       // currentPool - fixedPrizeTotal (can be negative)
    private boolean negativePoolException;

    @Column(precision = 20, scale = 6)
    private BigDecimal tier1Alloc;
    @Column(precision = 20, scale = 6)
    private BigDecimal tier2Alloc;
    @Column(precision = 20, scale = 6)
    private BigDecimal tier3Alloc;

    private int tier1WinningTickets;
    private int tier2WinningTickets;
    private int tier3WinningTickets;
    private int tier4WinningTickets;
    private int tier5WinningTickets;
    private int noWinTickets;

    @Column(precision = 20, scale = 6)
    private BigDecimal tier1PayoutPerTicket;
    @Column(precision = 20, scale = 6)
    private BigDecimal tier2PayoutPerTicket;
    @Column(precision = 20, scale = 6)
    private BigDecimal tier3PayoutPerTicket;

    /** Set only when negativePoolException is true. */
    @Column(precision = 20, scale = 6)
    private BigDecimal equalPayoutPerTicket;

    @Column(precision = 20, scale = 6)
    private BigDecimal totalPaidOut;
    @Column(precision = 20, scale = 6)
    private BigDecimal carryOutPool; // amount carried into the next round

    private Long settledAt;

    public Map<String, Object> toJson() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("roundNumber", roundNumber);
        m.put("status", status);
        m.put("startTime", startTime);
        m.put("endTime", endTime);
        m.put("carryInPool", MoneyFormat.plainStr(carryInPool));
        if (poolOverride != null) m.put("poolOverride", MoneyFormat.plainStr(poolOverride));
        if (drawnGeneralBalls != null) m.put("drawnGeneralBalls", drawnGeneralBalls);
        if (drawnPowerball != null) m.put("drawnPowerball", drawnPowerball);
        putIfPresent(m, "basicGameSales", basicGameSales);
        putIfPresent(m, "currentPool", currentPool);
        putIfPresent(m, "fixedPrizeTotal", fixedPrizeTotal);
        putIfPresent(m, "remainingPool", remainingPool);
        m.put("negativePoolException", negativePoolException);
        putIfPresent(m, "tier1Alloc", tier1Alloc);
        putIfPresent(m, "tier2Alloc", tier2Alloc);
        putIfPresent(m, "tier3Alloc", tier3Alloc);
        m.put("tier1WinningTickets", tier1WinningTickets);
        m.put("tier2WinningTickets", tier2WinningTickets);
        m.put("tier3WinningTickets", tier3WinningTickets);
        m.put("tier4WinningTickets", tier4WinningTickets);
        m.put("tier5WinningTickets", tier5WinningTickets);
        m.put("noWinTickets", noWinTickets);
        putIfPresent(m, "tier1PayoutPerTicket", tier1PayoutPerTicket);
        putIfPresent(m, "tier2PayoutPerTicket", tier2PayoutPerTicket);
        putIfPresent(m, "tier3PayoutPerTicket", tier3PayoutPerTicket);
        putIfPresent(m, "equalPayoutPerTicket", equalPayoutPerTicket);
        putIfPresent(m, "totalPaidOut", totalPaidOut);
        putIfPresent(m, "carryOutPool", carryOutPool);
        if (settledAt != null) m.put("settledAt", settledAt);
        return m;
    }

    private void putIfPresent(Map<String, Object> m, String key, BigDecimal v) {
        if (v != null) m.put(key, MoneyFormat.plainStr(v));
    }
}
