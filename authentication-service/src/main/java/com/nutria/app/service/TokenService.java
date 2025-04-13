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
                .map(t -> !t.isRevoked() || !t.isExpired())
                .orElse(false);
    }

    public void revokeToken(String token) {
        tokenRepository.findByToken(cleanToken(token))
                .ifPresent(t -> {
                    t.setRevoked(true);
                    tokenRepository.save(t);
                });
    }

}
