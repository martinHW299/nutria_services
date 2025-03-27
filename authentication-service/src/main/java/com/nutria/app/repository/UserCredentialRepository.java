package com.nutria.app.repository;

import com.nutria.app.model.UserCredential;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserCredentialRepository extends JpaRepository<UserCredential, Long> {
    Optional<UserCredential> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("SELECT u FROM UserCredential u WHERE u.email = :email AND u.status = 'ACTIVE'")
    Optional<UserCredential> findActiveUserByEmail(String email);

    long countByStatus(UserCredential.UserStatus status);
}