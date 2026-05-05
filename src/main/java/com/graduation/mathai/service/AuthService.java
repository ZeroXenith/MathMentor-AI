package com.graduation.mathai.service;

import com.graduation.mathai.dto.Requests.AuthResponse;
import com.graduation.mathai.model.AppUser;
import com.graduation.mathai.repository.AppUserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthService {
    private final AppUserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final Map<String, Long> sessions = new ConcurrentHashMap<>();

    public AuthService(AppUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public AuthResponse register(String username, String password) {
        String normalizedUsername = normalizeUsername(username);
        validatePassword(password);
        if (userRepository.existsByUsername(normalizedUsername)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "用户名已存在，请直接登录");
        }

        AppUser user = new AppUser();
        user.setUsername(normalizedUsername);
        user.setPasswordHash(passwordEncoder.encode(password));
        AppUser savedUser = userRepository.saveAndFlush(user);
        return createSession(savedUser);
    }

    public AuthResponse login(String username, String password) {
        String normalizedUsername = normalizeUsername(username);
        AppUser user = userRepository.findByUsername(normalizedUsername)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户名或密码错误"));
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户名或密码错误");
        }
        return createSession(user);
    }

    public long requireUserId(String token) {
        if (!StringUtils.hasText(token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "请先登录");
        }
        Long userId = sessions.get(token);
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "登录状态已失效，请重新登录");
        }
        return userId;
    }

    private AuthResponse createSession(AppUser user) {
        if (user.getId() <= 0) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "用户会话创建失败，请重新登录");
        }
        String token = UUID.randomUUID().toString().replace("-", "");
        sessions.put(token, user.getId());
        return new AuthResponse(token, user.getId(), user.getUsername());
    }

    private String normalizeUsername(String username) {
        if (!StringUtils.hasText(username)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "用户名不能为空");
        }
        String normalized = username.trim();
        if (normalized.length() < 3 || normalized.length() > 32) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "用户名长度应为 3 到 32 位");
        }
        if (!normalized.matches("[A-Za-z0-9_\\-\\u4e00-\\u9fa5]+")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "用户名只能包含中文、字母、数字、下划线或短横线");
        }
        return normalized;
    }

    private void validatePassword(String password) {
        if (!StringUtils.hasText(password) || password.length() < 6) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "密码至少需要 6 位");
        }
    }
}
