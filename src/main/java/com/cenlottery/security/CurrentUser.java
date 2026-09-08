package com.cenlottery.security;

import com.cenlottery.exception.ApiException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** Small helper so controllers can read "the logged-in user" without touching Spring Security types directly. */
@Component
public class CurrentUser {

    /** Returns the authenticated user id, or null for an anonymous request. */
    public String idOrNull() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AuthenticatedUser u)) return null;
        return u.userId();
    }

    /** Returns the authenticated user id, throwing the same error the endpoint would return if the token were missing/expired. */
    public String requireId() {
        String id = idOrNull();
        if (id == null) {
            throw ApiException.unauthorized("SESSION_EXPIRED", "로그인이 필요하거나 세션이 만료되었습니다. 다시 로그인해 주세요.");
        }
        return id;
    }
}
