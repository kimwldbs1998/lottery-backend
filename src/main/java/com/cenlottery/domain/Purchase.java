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
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "purchases", uniqueConstraints = @UniqueConstraint(columnNames = "idempotency_key"))
@Getter
@Setter
@NoArgsConstructor
public class Purchase {
    @Id
    private String id;

    @Column(nullable = false)
    private String userId;
    @Column(nullable = false)
    private long roundNumber;

    @ElementCollection
    @CollectionTable(name = "purchase_general_balls", joinColumns = @JoinColumn(name = "purchase_id"))
    @OrderColumn(name = "idx")
    @Column(name = "ball")
    private List<Integer> generalBalls;

    @ElementCollection
    @CollectionTable(name = "purchase_powerballs", joinColumns = @JoinColumn(name = "purchase_id"))
    @OrderColumn(name = "idx")
    @Column(name = "powerball")
    private List<Integer> powerballs;

    private boolean powerUp;
    private boolean allSelected;

    @Column(nullable = false, precision = 20, scale = 6)
    private BigDecimal pricePerGame;
    private int gameCount;
    @Column(nullable = false, precision = 20, scale = 6)
    private BigDecimal totalAmount;
    @Column(nullable = false, precision = 20, scale = 6)
    private BigDecimal balanceBefore;
    @Column(nullable = false, precision = 20, scale = 6)
    private BigDecimal balanceAfter;

    @ElementCollection
    @CollectionTable(name = "purchase_ticket_ids", joinColumns = @JoinColumn(name = "purchase_id"))
    @OrderColumn(name = "idx")
    @Column(name = "ticket_id")
    private List<String> ticketIds;

    @Column(name = "idempotency_key", nullable = false)
    private String idempotencyKey;

    private long createdAt;

    public Map<String, Object> toJson() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("userId", userId);
        m.put("roundNumber", roundNumber);
        m.put("generalBalls", generalBalls);
        m.put("powerballs", powerballs);
        m.put("powerUp", powerUp);
        m.put("allSelected", allSelected);
        m.put("pricePerGame", MoneyFormat.plainStr(pricePerGame));
        m.put("gameCount", gameCount);
        m.put("totalAmount", MoneyFormat.intStr(totalAmount));
        m.put("balanceBefore", MoneyFormat.intStr(balanceBefore));
        m.put("balanceAfter", MoneyFormat.intStr(balanceAfter));
        m.put("ticketIds", ticketIds);
        m.put("idempotencyKey", idempotencyKey);
        m.put("createdAt", createdAt);
        return m;
    }
}
