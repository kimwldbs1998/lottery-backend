package com.cenlottery.web;

import com.cenlottery.domain.Purchase;
import com.cenlottery.domain.SavedNumber;
import com.cenlottery.exception.ApiException;
import com.cenlottery.security.CurrentUser;
import com.cenlottery.service.PurchaseService;
import com.cenlottery.service.RoundService;
import com.cenlottery.service.SavedNumberService;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/purchase")
public class PurchaseController {
    private final PurchaseService purchaseService;
    private final SavedNumberService savedNumberService;
    private final RoundService roundService;
    private final CurrentUser currentUser;

    public PurchaseController(PurchaseService purchaseService, SavedNumberService savedNumberService,
                               RoundService roundService, CurrentUser currentUser) {
        this.purchaseService = purchaseService;
        this.savedNumberService = savedNumberService;
        this.roundService = roundService;
        this.currentUser = currentUser;
    }

    public record SelectionRequest(List<Integer> generalBalls, List<Integer> powerballs,
                                    Boolean allSelected, Boolean powerUp) {
    }

    public record ConfirmRequest(List<Integer> generalBalls, List<Integer> powerballs,
                                  Boolean allSelected, Boolean powerUp,
                                  Long roundNumber, String idempotencyKey) {
    }

    @PostMapping("/quote")
    public Map<String, Object> quote(@RequestBody SelectionRequest body) {
        PurchaseService.Selection sel = purchaseService.validate(
                body.generalBalls(), body.powerballs(), body.allSelected(), body.powerUp());
        return purchaseService.quote(currentUser.requireId(), sel);
    }

    @PostMapping("/confirm")
    public Map<String, Object> confirm(@RequestBody ConfirmRequest body) {
        PurchaseService.Selection sel = purchaseService.validate(
                body.generalBalls(), body.powerballs(), body.allSelected(), body.powerUp());
        if (body.roundNumber() == null) {
            throw ApiException.badRequest("ROUND_REQUIRED", "구매할 회차 정보가 없습니다.");
        }
        Purchase p = purchaseService.confirm(currentUser.requireId(), body.roundNumber(), sel, body.idempotencyKey());
        Map<String, Object> resp = new LinkedHashMap<>(p.toJson());
        resp.put("saleEndsAt", roundService.getRound(p.getRoundNumber()).getEndTime());
        return resp;
    }

    @GetMapping("/saved-number")
    public Map<String, Object> getSaved() {
        SavedNumber s = savedNumberService.get(currentUser.requireId());
        if (s == null) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("exists", false);
            return m;
        }
        return mergeExists(s.toJson());
    }

    @PostMapping("/saved-number")
    public Map<String, Object> saveSaved(@RequestBody SelectionRequest body) {
        String userId = currentUser.requireId();
        PurchaseService.Selection sel = purchaseService.validate(
                body.generalBalls(), body.powerballs(), body.allSelected(), null);
        boolean hadExisting = savedNumberService.get(userId) != null;
        SavedNumber saved = savedNumberService.save(userId, sel);
        Map<String, Object> resp = mergeExists(saved.toJson());
        resp.put("replaced", hadExisting);
        return resp;
    }

    @DeleteMapping("/saved-number")
    public Map<String, Object> deleteSaved() {
        savedNumberService.delete(currentUser.requireId());
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("deleted", true);
        return m;
    }

    private Map<String, Object> mergeExists(Map<String, Object> m) {
        Map<String, Object> out = new LinkedHashMap<>(m);
        out.put("exists", true);
        return out;
    }
}
