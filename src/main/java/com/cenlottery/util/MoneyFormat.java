package com.cenlottery.util;

import java.math.BigDecimal;
import java.math.BigInteger;

/** Renders BigDecimal amounts as JSON strings, matching the wire format the existing React frontend expects. */
public final class MoneyFormat {
    private MoneyFormat() {
    }

    /** Truncates to a whole-rupee integer string. Used for user-facing amounts (balances, prices, payouts). */
    public static String intStr(BigDecimal amount) {
        return amount == null ? null : amount.toBigInteger().toString();
    }

    /** Keeps full precision. Used for internal round accounting fields that can carry fractional rupees. */
    public static String plainStr(BigDecimal amount) {
        return amount == null ? null : amount.toPlainString();
    }

    public static String formatThousands(BigDecimal amount) {
        BigInteger whole = amount.toBigInteger();
        String s = whole.abs().toString();
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (int i = s.length() - 1; i >= 0; i--) {
            sb.append(s.charAt(i));
            count++;
            if (count % 3 == 0 && i != 0) sb.append(',');
        }
        String out = sb.reverse().toString();
        return (whole.signum() < 0 ? "-" : "") + out;
    }
}
