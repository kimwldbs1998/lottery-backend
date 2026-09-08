package com.cenlottery.security;

/** Principal stored in the SecurityContext for a JWT-authenticated request. */
public record AuthenticatedUser(String userId, String username) {
}
