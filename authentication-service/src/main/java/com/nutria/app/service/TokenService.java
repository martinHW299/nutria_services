package com.nutria.app.service;

import com.nutria.app.model.Token;
import com.nutria.app.model.UserCredential;
import com.nutria.app.repository.TokenRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class TokenService {

    private final TokenRepository tokenRepository;
    private final WebClient.Builder webClientBuilder;

    @Value("${application.gateway.url:http://api-gateway}")
    private String gatewayUrl;

    public String cleanToken(String token) {
        return token.replace("Bearer ", "").trim();
    }

    public void saveToken(String tokenValue, UserCredential userCredential) {
        Token token = Token.builder()
                .token(tokenValue)
                .userCredential(userCredential)
                .tokenType(Token.TokenType.BEARER)
                .revoked(false)
                .expired(false)
                .build();

        tokenRepository.save(token);
    }

    public boolean isTokenValid(String token) {
        return tokenRepository.findByToken(cleanToken(token))
                .map(t -> !t.isRevoked() && !t.isExpired())
                .orElse(false);
    }

    public void revokeToken(String token) {
        String cleanedToken = cleanToken(token);

        tokenRepository.findByToken(cleanedToken)
                .ifPresent(t -> {
                    t.setRevoked(true);
                    tokenRepository.save(t);
                    log.info("Token revoked in database: {}", cleanedToken.substring(0, 10) + "...");

                    // Notify API Gateway about token revocation
                    notifyGatewayTokenRevocation("Bearer " + cleanedToken)
                            .subscribe(
                                    success -> log.info("Successfully notified gateway about token revocation"),
                                    error -> log.warn("Failed to notify gateway about token revocation: {}", error.getMessage())
                            );
                });
    }

    public void revokeAllUserTokens(UserCredential userCredential) {
        List<Token> userTokens = tokenRepository.findAllByUserCredentialAndRevokedFalse(userCredential);

        userTokens.forEach(token -> {
            token.setRevoked(true);
            tokenRepository.save(token);

            // Notify gateway about each token revocation
            notifyGatewayTokenRevocation("Bearer " + token.getToken())
                    .subscribe(
                            success -> log.debug("Notified gateway about token revocation"),
                            error -> log.warn("Failed to notify gateway about token revocation: {}", error.getMessage())
                    );
        });

        log.info("Revoked {} tokens for user: {}", userTokens.size(), userCredential.getEmail());
    }

    public int countValidTokensByUser(UserCredential userCredential) {
        return tokenRepository.countValidTokensByUser(userCredential);
    }

    private Mono<Void> notifyGatewayTokenRevocation(String token) {
        return webClientBuilder.build()
                .post()
                .uri(gatewayUrl + "/api/v1/gateway/tokens/revoke")
                .header("Authorization", token)
                .retrieve()
                .bodyToMono(Void.class)
                .onErrorResume(error -> {
                    log.warn("Gateway notification failed, but token is revoked in database");
                    return Mono.empty();
                });
    }
}