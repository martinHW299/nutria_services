package com.nutria.app.filter;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Predicate;

@Component
public class RouteValidator {

    public static final List<String> openApiEndPoints = List.of(
            "/api/v1/auth/login",
            "/api/v1/auth/signup",
            "/api/v1/auth/validate",
            "/api/v1/auth/advisor",
            "/api/v1/auth/logout",
            "/api/v1/auth/health",
            "/api/v1/gateway/tokens/cache/stats",
            "/eureka"
    );

    public Predicate<ServerHttpRequest> isSecured =
            request -> openApiEndPoints
                    .stream()
                    .noneMatch(uri -> request.getURI().getPath().contains(uri));
}
