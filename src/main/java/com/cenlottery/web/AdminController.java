package com.cenlottery.web;

import com.cenlottery.config.AdminKeyHolder;
import com.cenlottery.domain.Round;
import com.cenlottery.exception.ApiException;
import com.cenlottery.service.RoundService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * QA/testing-only endpoints, not part of the customer-facing spec. Requirement section 7 notes
 * that the negative-pool exception cannot occur under normal play with the fixed constants given,
 * so a way to seed an artificial pool value is needed to exercise and verify that code path; a
 * force-settle hook also makes it practical to test the 5-minute draw cycle without waiting.
 * Guarded by a shared admin key so it can't be hit accidentally from the public game screen.
 */
@RestController
@RequestMapping("/api/admin/rounds/current")
public class AdminController {
    private final RoundService roundService;
    private final AdminKeyHolder adminKeyHolder;

    public AdminController(RoundService roundService, AdminKeyHolder adminKeyHolder) {
        this.roundService = roundService;
        this.adminKeyHolder = adminKeyHolder;
    }

    public record PoolOverrideRequest(String amount) {
    }

    @PostMapping("/force-settle")
    public Map<String, Object> forceSettle(@RequestHeader(value = "X-Admin-Key", required = false) String adminKey) {
        checkKey(adminKey);
        Round settled = roundService.forceSettleCurrentRound();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("settledRound", settled.toJson());
        m.put("newRound", roundService.getCurrentRound().toJson());
        return m;
    }

    @PostMapping("/pool-override")
    public Map<String, Object> poolOverride(@RequestHeader(value = "X-Admin-Key", required = false) String adminKey,
                                             @RequestBody PoolOverrideRequest body) {
        checkKey(adminKey);
        if (body.amount() == null) throw ApiException.badRequest("AMOUNT_REQUIRED", "amount 값이 필요합니다.");
        Round r = roundService.overrideCurrentRoundPool(new BigDecimal(body.amount()));
        return r.toJson();
    }

    private void checkKey(String header) {
        if (adminKeyHolder.getKey() == null || !adminKeyHolder.getKey().equals(header)) {
            throw ApiException.unauthorized("ADMIN_KEY_INVALID", "관리자 키가 올바르지 않습니다.");
        }
    }
}
