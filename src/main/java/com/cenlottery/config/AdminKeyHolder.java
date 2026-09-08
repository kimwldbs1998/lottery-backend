package com.cenlottery.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

/** Holds the shared secret required by /api/admin/* test endpoints (see AdminController). */
@Component
public class AdminKeyHolder {
    private final String key;

    public AdminKeyHolder(@Value("${app.admin-key:}") String configuredKey) {
        this.key = (configuredKey == null || configuredKey.isBlank()) ? generateKey() : configuredKey;
    }

    public String getKey() {
        return key;
    }

    private static String generateKey() {
        byte[] b = new byte[12];
        new SecureRandom().nextBytes(b);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }
}
