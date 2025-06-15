package com.nutria.app.controller;

import com.nutria.app.util.TokenCacheService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/gateway/tokens")
@RequiredArgsConstructor
public class TokenRevocationController {

    private final TokenCacheService tokenCacheService;

    /**
     * Endpoint for authentication service to notify gateway about token revocation
     */
    @PostMapping("/revoke")
    public ResponseEntity<Void> revokeToken(@RequestHeader(HttpHeaders.AUTHORIZATION) String token) {
        tokenCacheService.revokeToken(token);
        return ResponseEntity.ok().build();
    }

    /**
     * Endpoint to revoke a specific token by value
     */
    @PostMapping("/revoke/by-token")
    public ResponseEntity<Void> revokeTokenByValue(@RequestBody TokenRevocationRequest request) {
        tokenCacheService.revokeToken(request.getToken());
        return ResponseEntity.ok().build();
    }

    /**
     * Get cache statistics (for monitoring purposes)
     */
    @GetMapping("/cache/stats")
    public ResponseEntity<TokenCacheStats> getCacheStats() {
        TokenCacheStats stats = TokenCacheStats.builder()
                .revokedTokenCount(tokenCacheService.getRevokedTokenCount())
                .build();
        return ResponseEntity.ok(stats);
    }

    /**
     * Clear cache (admin endpoint)
     */
    @DeleteMapping("/cache/clear")
    public ResponseEntity<Void> clearCache() {
        tokenCacheService.clearCache();
        return ResponseEntity.ok().build();
    }

    @Getter
    public static class TokenRevocationRequest {
        private String token;
        public void setToken(String token) {
            this.token = token;
        }
    }

    @Getter
    public static class TokenCacheStats {
        private int revokedTokenCount;

        public static TokenCacheStatsBuilder builder() {
            return new TokenCacheStatsBuilder();
        }

        public void setRevokedTokenCount(int revokedTokenCount) {
            this.revokedTokenCount = revokedTokenCount;
        }

        public static class TokenCacheStatsBuilder {
            private int revokedTokenCount;

            public TokenCacheStatsBuilder revokedTokenCount(int revokedTokenCount) {
                this.revokedTokenCount = revokedTokenCount;
                return this;
            }

            public TokenCacheStats build() {
                TokenCacheStats stats = new TokenCacheStats();
                stats.setRevokedTokenCount(this.revokedTokenCount);
                return stats;
            }
        }
    }
}