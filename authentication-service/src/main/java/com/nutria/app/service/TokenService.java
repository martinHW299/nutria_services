package com.nutria.app.service;

import com.nutria.app.model.Token;
import com.nutria.app.model.UserCredential;
import com.nutria.app.repository.TokenRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class TokenService {

    private final TokenRepository tokenRepository;

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
        log.info("validate token: {}", tokenRepository.findByToken(token));
        return tokenRepository.findByToken(token)
                .map(t -> !t.isRevoked() && !t.isExpired())
                .orElse(false);
    }

    public void revokeToken(String token) {
        log.info("revoke token: {}", tokenRepository.findByToken(token));
        tokenRepository.findByToken(token)
                .ifPresent(t -> {
                    t.setRevoked(true);
                    tokenRepository.save(t);
                });
    }

    public void revokeAllUserTokens(UserCredential userCredential) {
        List<Token> validTokens = tokenRepository.findAllValidTokensByUser(userCredential.getId());

        if (validTokens.isEmpty()) {
            return;
        }

        validTokens.forEach(t -> {
            t.setRevoked(true);
            // You could also set expired=true if needed
        });

        tokenRepository.saveAll(validTokens);
    }

}
