package com.nutria.app.repository;

import com.nutria.app.model.Token;
import com.nutria.app.model.UserCredential;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TokenRepository extends JpaRepository<Token, Long> {

    Optional<Token> findByToken(String token);

    List<Token> findAllByUserCredentialAndRevokedFalse(UserCredential userCredential);

    @Query("SELECT t FROM Token t WHERE t.userCredential = :userCredential AND t.revoked = false AND t.expired = false")
    List<Token> findAllValidTokensByUser(@Param("userCredential") UserCredential userCredential);

    @Query("SELECT COUNT(t) FROM Token t WHERE t.userCredential = :userCredential AND t.revoked = false AND t.expired = false")
    int countValidTokensByUser(@Param("userCredential") UserCredential userCredential);
}