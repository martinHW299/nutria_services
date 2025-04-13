package com.nutria.app.filter;


import com.nutria.app.util.JwtUtil;
import jakarta.validation.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHeaders;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.stereotype.Component;
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
            if (!routeValidator.isSecured.test(exchange.getRequest())) {
                return chain.filter(exchange);
            }

            String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (!authHeader.startsWith("Bearer ")) {
                return Mono.error(new ValidationException("Missing or invalid Authorization header"));
            }

            String token = authHeader.substring(7).trim();

            return jwtUtil.validateToken(token)
                    .flatMap(isValid -> {
                        if (!isValid) {
                            return Mono.error(new ValidationException("Unauthorized access to application, token not valid"));
                        }
                        return chain.filter(exchange);
                    })
                    .onErrorResume(e -> {
                        return Mono.error(new ValidationException("Unauthorized access to application"));
                    });
        };
    }


    public static class Config {}
}
