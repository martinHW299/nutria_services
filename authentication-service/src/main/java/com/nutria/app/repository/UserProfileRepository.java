package com.nutria.app.repository;

import com.nutria.app.model.UserCredential;
import com.nutria.app.model.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {

    Optional<UserProfile> findUserProfileByUserCredential(UserCredential userCredential);
    
}
