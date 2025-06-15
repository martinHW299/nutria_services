package com.nutria.app.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class TokenCacheService {

    private final ConcurrentHashMap<String, LocalDateTime> revokedTokens = new ConcurrentHashMap<>();

    @Value("${application.security.jwt.cache.cleanup-hours:24}")
    private int cleanupHours;

    /**
     * Add a token to the revoked tokens cache
     */
    public void revokeToken(String token) {
        String cleanToken = cleanToken(token);
        revokedTokens.put(cleanToken, LocalDateTime.now());
        log.info("Token revoked and cached: {}", cleanToken.substring(0, 10) + "...");
    }

    /**
     * Check if a token is revoked
     */
    public boolean isTokenRevoked(String token) {
        String cleanToken = cleanToken(token);
        return revokedTokens.containsKey(cleanToken);
    }

    /**
     * Remove a token from the revoked cache (used when token expires naturally)
     */
    public void removeFromCache(String token) {
        String cleanToken = cleanToken(token);
        revokedTokens.remove(cleanToken);
    }

    /**
     * Get the count of cached revoked tokens
     */
    public int getRevokedTokenCount() {
        return revokedTokens.size();
    }

    /**
     * Clean up old revoked tokens every hour
     */
    @Scheduled(fixedRate = 3600000) // 1 hour
    public void cleanupExpiredRevokedTokens() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(cleanupHours);
        int initialSize = revokedTokens.size();

        revokedTokens.entrySet().removeIf(entry -> entry.getValue().isBefore(cutoff));

        int removedCount = initialSize - revokedTokens.size();
        if (removedCount > 0) {
            log.info("Cleaned up {} expired revoked tokens from cache", removedCount);
        }
    }

    /**
     * Clear all revoked tokens from cache (for admin purposes)
     */
    public void clearCache() {
        int size = revokedTokens.size();
        revokedTokens.clear();
        log.info("Cleared all {} revoked tokens from cache", size);
    }

    private String cleanToken(String token) {
        if (token == null) {
            return "";
        }
        return token.replace("Bearer ", "").trim();
    }
}