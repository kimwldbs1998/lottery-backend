package com.cenlottery.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Requirement section 9 "나만의 번호": exactly one saved favorite number per user. */
@Entity
@Table(name = "saved_numbers")
@Getter
@Setter
@NoArgsConstructor
public class SavedNumber {
    @Id
    private String userId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "saved_number_general_balls", joinColumns = @JoinColumn(name = "user_id"))
    @OrderColumn(name = "idx")
    @Column(name = "ball")
    private List<Integer> generalBalls;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "saved_number_powerballs", joinColumns = @JoinColumn(name = "user_id"))
    @OrderColumn(name = "idx")
    @Column(name = "powerball")
    private List<Integer> powerballs;

    private long updatedAt;

    public Map<String, Object> toJson() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("userId", userId);
        m.put("generalBalls", generalBalls);
        m.put("powerballs", powerballs);
        m.put("updatedAt", updatedAt);
        return m;
    }
}
