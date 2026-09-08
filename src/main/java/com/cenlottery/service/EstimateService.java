package com.cenlottery.service;

import com.cenlottery.domain.Round;
import com.cenlottery.domain.Ticket;
import com.cenlottery.util.Constants;
import com.cenlottery.util.MoneyFormat;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Implements requirement section 8: the pre-draw estimated 1st/2nd place prize display.
 * This is a display-only projection using win-probability-weighted expected fixed prizes;
 * the real payout always comes from SettlementService once the round actually closes.
 */
@Service
public class EstimateService {

    public Map<String, Object> estimate(Round round, List<Ticket> ticketsSoFar) {
        int normalCount = 0;
        int powerUpCount = 0;
        for (Ticket t : ticketsSoFar) {
            if (t.isPowerUp()) powerUpCount++; else normalCount++;
        }

        BigDecimal carryIn = round.getPoolOverride() != null ? round.getPoolOverride() : round.getCarryInPool();
        BigDecimal basicSales = Constants.NORMAL_PRICE.multiply(BigDecimal.valueOf(normalCount + powerUpCount));
        BigDecimal currentPoolEstimate = carryIn.add(basicSales.multiply(Constants.POOL_SALES_RATIO));

        BigDecimal expectedFixedTotal = Constants.EXPECTED_FIXED_NORMAL.multiply(BigDecimal.valueOf(normalCount))
                .add(Constants.EXPECTED_FIXED_POWERUP.multiply(BigDecimal.valueOf(powerUpCount)));

        BigDecimal expectedRemainingPool = currentPoolEstimate.subtract(expectedFixedTotal);
        boolean negative = expectedRemainingPool.signum() < 0;
        BigDecimal effectiveRemaining = negative ? BigDecimal.ZERO : expectedRemainingPool;

        BigDecimal tier1Expected = effectiveRemaining.multiply(Constants.TIER1_RATIO).setScale(0, RoundingMode.DOWN);
        BigDecimal tier2Expected = effectiveRemaining.multiply(Constants.TIER2_RATIO).setScale(0, RoundingMode.DOWN);

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("roundNumber", round.getRoundNumber());
        m.put("tier1Expected", MoneyFormat.intStr(tier1Expected));
        m.put("tier2Expected", MoneyFormat.intStr(tier2Expected));
        m.put("carryInPool", MoneyFormat.intStr(carryIn));
        m.put("isEstimate", true);
        m.put("note", "실제 지급액은 당첨 매수와 정산 결과에 따라 달라집니다.");
        m.put("lastUpdated", System.currentTimeMillis());
        return m;
    }
}
