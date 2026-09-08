package com.cenlottery.domain;

import com.cenlottery.util.MoneyFormat;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

@Entity
@Table(name = "users", uniqueConstraints = @UniqueConstraint(columnNames = "username_lower"))
@Getter
@Setter
@NoArgsConstructor
public class User {
    @Id
    private String id;

    @Column(nullable = false)
    private String username;

    /** Lowercased username, used for case-insensitive uniqueness/lookup (mirrors the original in-memory index). */
    @Column(name = "username_lower", nullable = false)
    private String usernameLower;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false, precision = 20, scale = 2)
    private BigDecimal balance;

    @Column(nullable = false)
    private long createdAt;

    public Map<String, Object> toPublicJson() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("username", username);
        m.put("balance", MoneyFormat.intStr(balance));
        m.put("balanceDisplay", "Rs " + MoneyFormat.formatThousands(balance));
        return m;
    }
}
