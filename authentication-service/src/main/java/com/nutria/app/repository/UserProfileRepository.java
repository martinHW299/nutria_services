package com.nutria.app.repository;

import com.nutria.app.model.UserCredential;
import com.nutria.app.model.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {

    UserProfile findUserProfileByUserCredential(UserCredential userCredential);
}
