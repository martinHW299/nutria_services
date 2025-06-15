package com.nutria.app.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Slf4j
@Configuration
public class WebClientConfig {

    @Bean
    @LoadBalanced
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024))
                .filter(logRequest())
                .filter(logResponse())
                .build()
                .mutate();
    }

    /**
     * WebClient for gateway communication with load balancing
     */
    @Bean
    public WebClient gatewayWebClient(WebClient.Builder webClientBuilder) {
        return webClientBuilder
                .baseUrl("http://api-gateway") // Service name for Eureka
                .build();
    }

    /**
     * Log outgoing requests
     */
    private ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            if (log.isDebugEnabled()) {
                log.debug("Request: {} {}", clientRequest.method(), clientRequest.url());
                clientRequest.headers().forEach((name, values) ->
                        values.forEach(value -> log.debug("{}={}", name, value)));
            }
            return Mono.just(clientRequest);
        });
    }

    /**
     * Log incoming responses
     */
    private ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
            if (log.isDebugEnabled()) {
                log.debug("Response status: {}", clientResponse.statusCode());
            }
            return Mono.just(clientResponse);
        });
    }
}