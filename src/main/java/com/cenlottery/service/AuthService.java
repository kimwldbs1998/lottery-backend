package com.cenlottery.service;

import com.cenlottery.domain.User;
import com.cenlottery.exception.ApiException;
import com.cenlottery.repository.UserRepository;
import com.cenlottery.security.JwtService;
import com.cenlottery.util.Constants;
import com.cenlottery.util.IdGenerator;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public record AuthResult(String token, User user) {
    }

    @Transactional
    public AuthResult register(String username, String password) {
        if (username == null || username.isBlank() || username.length() < 3 || username.length() > 30) {
            throw ApiException.badRequest("INVALID_USERNAME", "아이디는 3~30자로 입력해 주세요.");
        }
        if (password == null || password.length() < 4) {
            throw ApiException.badRequest("INVALID_PASSWORD", "비밀번호는 4자 이상 입력해 주세요.");
        }
        String key = username.toLowerCase(Locale.ROOT);
        if (userRepository.existsByUsernameLower(key)) {
            throw ApiException.conflict("USERNAME_TAKEN", "이미 사용 중인 아이디입니다.");
        }
        User u = new User();
        u.setId(IdGenerator.next("user"));
        u.setUsername(username);
        u.setUsernameLower(key);
        u.setPasswordHash(passwordEncoder.encode(password));
        u.setBalance(Constants.INITIAL_BALANCE);
        u.setCreatedAt(System.currentTimeMillis());
        userRepository.save(u);
        String token = jwtService.issue(u.getId(), u.getUsername());
        return new AuthResult(token, u);
    }

    public AuthResult login(String username, String password) {
        if (username == null || password == null) {
            throw ApiException.badRequest("INVALID_CREDENTIALS", "아이디와 비밀번호를 입력해 주세요.");
        }
        User u = userRepository.findByUsernameLower(username.toLowerCase(Locale.ROOT)).orElse(null);
        if (u == null || !passwordEncoder.matches(password, u.getPasswordHash())) {
            throw ApiException.badRequest("INVALID_CREDENTIALS", "아이디 또는 비밀번호가 올바르지 않습니다.");
        }
        String token = jwtService.issue(u.getId(), u.getUsername());
        return new AuthResult(token, u);
    }

    public User requireUser(String userId) {
        User u = userId == null ? null : userRepository.findById(userId).orElse(null);
        if (u == null) throw ApiException.unauthorized("SESSION_EXPIRED", "로그인이 필요합니다.");
        return u;
    }
}
