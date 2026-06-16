package com.graduation.mathai.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory rate limiter — 60 requests per minute per IP.
 * Suitable for small deployments; replace with Redis-based limiter for scale.
 */
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final int MAX_REQUESTS = 60;
    private static final long WINDOW_MS = 60_000L;

    private final Map<String, WindowCounter> counters = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String clientIp = getClientIp(request);
        long now = System.currentTimeMillis();

        WindowCounter counter = counters.compute(clientIp, (ip, existing) -> {
            if (existing == null || now - existing.windowStart > WINDOW_MS) {
                return new WindowCounter(now, 1);
            }
            existing.count++;
            return existing;
        });

        if (counter.count > MAX_REQUESTS) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json;charset=UTF-8");
            try {
                response.getWriter().write("{\"message\":\"请求过于频繁，请稍后再试。\"}");
            } catch (Exception ignored) {
                // Response already committed
            }
            return false;
        }

        return true;
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static class WindowCounter {
        final long windowStart;
        int count;

        WindowCounter(long windowStart, int count) {
            this.windowStart = windowStart;
            this.count = count;
        }
    }
}
