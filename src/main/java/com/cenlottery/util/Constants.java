package com.cenlottery.util;

import java.math.BigDecimal;

public final class Constants {
    private Constants() {
    }

    public static final int GENERAL_BALL_MIN = 1;
    public static final int GENERAL_BALL_MAX = 7;
    public static final int GENERAL_BALL_PICK = 5;

    public static final int POWERBALL_MIN = 0;
    public static final int POWERBALL_MAX = 9;
    public static final int POWERBALL_COUNT = 10; // 0..9

    public static final int MAX_TICKETS_PER_PURCHASE = 10;

    public static final BigDecimal NORMAL_PRICE = new BigDecimal("40");
    public static final BigDecimal POWERUP_SURCHARGE = new BigDecimal("20");
    public static final BigDecimal POWERUP_PRICE = new BigDecimal("60");

    /** Test/dev round length. Real production cadence (twice a week) is set by ops policy, not this build. */
    public static final long ROUND_DURATION_MS = 5L * 60 * 1000;

    public static final BigDecimal FIXED_4TH = new BigDecimal("10");
    public static final BigDecimal FIXED_4TH_POWERUP = new BigDecimal("20");
    public static final BigDecimal FIXED_5TH = new BigDecimal("5");
    public static final BigDecimal FIXED_5TH_POWERUP = new BigDecimal("10");

    public static final BigDecimal POOL_SALES_RATIO = new BigDecimal("0.5");
    public static final BigDecimal TIER1_RATIO = new BigDecimal("0.75");
    public static final BigDecimal TIER2_RATIO = new BigDecimal("0.125");
    public static final BigDecimal TIER3_RATIO = new BigDecimal("0.125");

    /** Expected fixed-prize-per-game constants used only for the pre-draw estimate display (section 8). */
    public static final BigDecimal EXPECTED_FIXED_NORMAL = new BigDecimal("95").divide(new BigDecimal("21"), 10, java.math.RoundingMode.HALF_UP);
    public static final BigDecimal EXPECTED_FIXED_POWERUP = new BigDecimal("190").divide(new BigDecimal("21"), 10, java.math.RoundingMode.HALF_UP);

    public static final BigDecimal INITIAL_BALANCE = new BigDecimal("100000");
}
