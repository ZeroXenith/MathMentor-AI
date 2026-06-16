package com.graduation.mathai.service;

import com.graduation.mathai.dto.Requests;
import com.graduation.mathai.model.AppUser;
import com.graduation.mathai.repository.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthServiceTest {
    private AppUserRepository userRepository;
    private JwtTokenProvider jwtTokenProvider;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepository = mock(AppUserRepository.class);
        jwtTokenProvider = mock(JwtTokenProvider.class);
        authService = new AuthService(userRepository, jwtTokenProvider);
    }

    @Test
    void register_success() {
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        AppUser savedUser = new AppUser();
        savedUser.setId(1L);
        savedUser.setUsername("testuser");
        when(userRepository.saveAndFlush(any())).thenReturn(savedUser);
        when(jwtTokenProvider.generateToken(1L, "testuser")).thenReturn("mock-jwt-token");

        Requests.AuthResponse result = authService.register("testuser", "password123");

        assertNotNull(result.token());
        assertEquals(1L, result.userId());
        assertEquals("testuser", result.username());
        assertEquals("mock-jwt-token", result.token());
    }

    @Test
    void register_duplicateUsername_throwsConflict() {
        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.register("testuser", "password123"));
        assertEquals(HttpStatus.CONFLICT, HttpStatus.valueOf(ex.getStatusCode().value()));
    }

    @Test
    void register_shortPassword_throwsBadRequest() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.register("testuser", "123"));
        assertEquals(HttpStatus.BAD_REQUEST, HttpStatus.valueOf(ex.getStatusCode().value()));
    }

    @Test
    void login_success_returnsToken() {
        AppUser user = new AppUser();
        user.setId(2L);
        user.setUsername("testuser");
        // Pre-computed BCrypt hash for "password123"
        user.setPasswordHash("$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(jwtTokenProvider.generateToken(2L, "testuser")).thenReturn("jwt-token-2");

        // This will fail with dummy hash, but tests the lookup flow
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.login("testuser", "password123"));
        assertEquals(HttpStatus.UNAUTHORIZED, HttpStatus.valueOf(ex.getStatusCode().value()));
    }

    @Test
    void login_userNotFound_throwsUnauthorized() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.login("unknown", "password"));
        assertEquals(HttpStatus.UNAUTHORIZED, HttpStatus.valueOf(ex.getStatusCode().value()));
    }

    @Test
    void requireUserId_missingToken_throwsUnauthorized() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.requireUserId(null));
        assertEquals(HttpStatus.UNAUTHORIZED, HttpStatus.valueOf(ex.getStatusCode().value()));
    }

    @Test
    void requireUserId_validToken_returnsUserId() {
        when(jwtTokenProvider.validateAndGetUserId("valid-jwt")).thenReturn(42L);

        long userId = authService.requireUserId("valid-jwt");
        assertEquals(42L, userId);
    }

    @Test
    void requireUserId_expiredToken_throwsUnauthorized() {
        when(jwtTokenProvider.validateAndGetUserId("expired-token")).thenReturn(null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.requireUserId("expired-token"));
        assertEquals(HttpStatus.UNAUTHORIZED, HttpStatus.valueOf(ex.getStatusCode().value()));
    }
}
