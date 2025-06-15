package com.nutria.app.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.security.Key;
import java.util.Date;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtUtil {

    @Value("${application.security.jwt.secret-key}")
    private String secretKey;

    private final TokenCacheService tokenCacheService;

    public Mono<Boolean> validateToken(final String token) {
        return Mono.fromCallable(() -> {
            try {
                // First check if token is in cache and revoked
                if (tokenCacheService.isTokenRevoked(token)) {
                    log.debug("Token is revoked: {}", token.substring(0, 10) + "...");
                    return false;
                }

                // Parse and validate JWT structure and signature
                Claims claims = Jwts.parser()
                        .setSigningKey(getSignInKey())
                        .build()
                        .parseClaimsJws(token)
                        .getBody();

                // Check expiration
                Date expiration = claims.getExpiration();
                if (expiration != null && expiration.before(new Date())) {
                    log.debug("Token is expired: {}", token.substring(0, 10) + "...");
                    return false;
                }

                log.debug("Token validation successful for: {}", claims.getSubject());
                return true;

            } catch (Exception e) {
                log.warn("Token validation failed: {}", e.getMessage());
                return false;
            }
        }).onErrorReturn(false);
    }

    public Mono<String> extractEmail(final String token) {
        return Mono.fromCallable(() -> {
                    try {
                        Claims claims = Jwts.parser()
                                .setSigningKey(getSignInKey())
                                .build()
                                .parseClaimsJws(token)
                                .getBody();
                        return claims.getSubject();
                    } catch (Exception e) {
                        log.warn("Failed to extract email from token: {}", e.getMessage());
                        return "";
                    }
                }).onErrorReturn("")
                .filter(email -> !email.isEmpty())
                .cast(String.class);
    }

    public Mono<Claims> extractAllClaims(final String token) {
        return Mono.fromCallable(() -> {
            try {
                return Jwts.parser()
                        .setSigningKey(getSignInKey())
                        .build()
                        .parseClaimsJws(token)
                        .getBody();
            } catch (Exception e) {
                log.warn("Failed to extract claims from token: {}", e.getMessage());
                return null;
            }
        }).filter(claims -> claims != null);
    }

    private Key getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}