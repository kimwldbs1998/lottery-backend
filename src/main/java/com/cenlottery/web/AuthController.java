package com.cenlottery.web;

import com.cenlottery.domain.User;
import com.cenlottery.security.CurrentUser;
import com.cenlottery.service.AuthService;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final CurrentUser currentUser;

    public AuthController(AuthService authService, CurrentUser currentUser) {
        this.authService = authService;
        this.currentUser = currentUser;
    }

    public record CredentialsRequest(String username, String password) {
    }

    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody CredentialsRequest body) {
        AuthService.AuthResult result = authService.register(body.username(), body.password());
        return response(result);
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody CredentialsRequest body) {
        AuthService.AuthResult result = authService.login(body.username(), body.password());
        return response(result);
    }

    @GetMapping("/me")
    public Map<String, Object> me() {
        User u = authService.requireUser(currentUser.requireId());
        return u.toPublicJson();
    }

    private Map<String, Object> response(AuthService.AuthResult result) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("token", result.token());
        m.put("user", result.user().toPublicJson());
        return m;
    }
}
