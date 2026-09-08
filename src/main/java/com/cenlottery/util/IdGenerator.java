package com.cenlottery.util;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public final class IdGenerator {
    private IdGenerator() {
    }

    private static final AtomicLong SEQ = new AtomicLong(0);

    public static String next(String prefix) {
        return prefix + "_" + System.currentTimeMillis() + "_" + SEQ.incrementAndGet() + "_" + UUID.randomUUID().toString().substring(0, 8);
    }
}
