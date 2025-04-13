package com.nutria.app.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.security.Key;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtUtil {

    @Value("${application.security.jwt.secret-key}")
    private String secretKey;

    private final WebClient.Builder webClientBuilder;

    public Mono<Boolean> validateToken(final String token) {
        try {
            Jwts.parser()
                    .setSigningKey(getSignInKey())
                    .build()
                    .parseClaimsJws(token); // Syntax & signature check
        } catch (Exception e) {
            return Mono.just(false);
        }

        //Map<String, String> body = Map.of("token", token);

        return webClientBuilder.build()
                .post()
                .uri("http://authentication-service/api/v1/auth/validate")
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .bodyToMono(Boolean.class)
                .onErrorReturn(false); // fallback if call fails
    }

    private Key getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

}
