package com.nutria.app.filter;

import com.nutria.app.util.JwtUtil;
import jakarta.validation.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHeaders;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class AuthFilter extends AbstractGatewayFilterFactory<AuthFilter.Config> {

    private final RouteValidator routeValidator;
    private final JwtUtil jwtUtil;

    public AuthFilter(RouteValidator routeValidator, JwtUtil jwtUtil) {
        super(Config.class);
        this.routeValidator = routeValidator;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            // Skip validation for unsecured routes
            if (!routeValidator.isSecured.test(exchange.getRequest())) {
                return chain.filter(exchange);
            }

            String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

            // Check if Authorization header exists and has correct format
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn("Missing or invalid Authorization header");
                return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid Authorization header"));
            }

            String token = authHeader.substring(7).trim();
            log.debug("Processing token: {}", token.substring(0, Math.min(10, token.length())) + "...");

            // Validate token using local JWT validation (no HTTP call)
            return jwtUtil.validateToken(token)
                    .flatMap(isValid -> {
                        if (!isValid) {
                            log.warn("Token validation failed for token: {}", token.substring(0, Math.min(10, token.length())) + "...");
                            return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired token"));
                        }

                        // Add user info to request headers for downstream services
                        return jwtUtil.extractEmail(token)
                                .map(email -> {
                                    // Add user email to headers for downstream services
                                    return exchange.getRequest().mutate()
                                            .header("X-User-Email", email)
                                            .build();
                                })
                                .defaultIfEmpty(exchange.getRequest()) // Continue even if email extraction fails
                                .flatMap(request -> chain.filter(exchange.mutate().request(request).build()));
                    })
                    .onErrorResume(ResponseStatusException.class, Mono::error)
                    .onErrorResume(e -> {
                        log.error("Unexpected error during token validation", e);
                        return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token validation failed"));
                    });
        };
    }

    public static class Config {
        // Configuration properties can be added here if needed
    }
}