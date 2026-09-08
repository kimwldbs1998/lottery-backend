package com.cenlottery.domain;

import com.cenlottery.util.MoneyFormat;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
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
@Table(name = "tickets")
@Getter
@Setter
@NoArgsConstructor
public class Ticket {
    public static final String PENDING = "PENDING_DRAW";
    public static final String SETTLED = "SETTLED";

    @Id
    private String id;

    @Column(nullable = false)
    private String purchaseId;
    @Column(nullable = false)
    private String userId;
    @Column(nullable = false)
    private long roundNumber;

    /** 5 distinct numbers in 1..7. */
    @ElementCollection
    @CollectionTable(name = "ticket_general_balls", joinColumns = @JoinColumn(name = "ticket_id"))
    @OrderColumn(name = "idx")
    @Column(name = "ball")
    private List<Integer> generalBalls;

    private int powerball; // 0..9
    private boolean powerUp;

    @Column(nullable = false, precision = 20, scale = 6)
    private BigDecimal pricePerGame;
    private long createdAt;

    @Column(nullable = false)
    private String status = PENDING;

    /** 1..5, or null for no-win (only meaningful once status == SETTLED). */
    private Integer tier;
    /** Final integer-rupee payout, set at settlement. */
    @Column(precision = 20, scale = 6)
    private BigDecimal payout;
    /** "NORMAL" or "EQUAL", set at settlement. */
    private String settlementType;

    public Map<String, Object> toJson() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("purchaseId", purchaseId);
        m.put("userId", userId);
        m.put("roundNumber", roundNumber);
        m.put("generalBalls", generalBalls);
        m.put("powerball", powerball);
        m.put("powerUp", powerUp);
        m.put("pricePerGame", MoneyFormat.plainStr(pricePerGame));
        m.put("createdAt", createdAt);
        m.put("status", status);
        if (tier != null) m.put("tier", tier);
        if (payout != null) m.put("payout", MoneyFormat.intStr(payout));
        if (settlementType != null) m.put("settlementType", settlementType);
        return m;
    }
}
