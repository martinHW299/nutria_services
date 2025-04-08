package com.nutria.app.service;

import com.nutria.app.dto.ChangePasswordRequest;
import com.nutria.app.dto.LoginRequest;
import com.nutria.app.dto.SignupRequest;
import com.nutria.app.model.UserCredential;
import com.nutria.app.model.UserProfile;
import com.nutria.app.repository.UserCredentialRepository;
import com.nutria.app.repository.UserProfileRepository;
import com.nutria.common.exceptions.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class UserService {

    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final UserCredentialRepository userCredentialRepository;
    private final AuthenticationManager authenticationManager;
    private final UserProfileRepository userProfileRepository;
    private final UserProfileService userProfileService;
    private final TokenService tokenService;

    public UserProfile signup(SignupRequest signupRequest) {
        validateSignupEmail(signupRequest.getEmail());

        UserCredential userCredential = createUserCredential(signupRequest);
        userCredentialRepository.save(userCredential);

        return userProfileService.saveUserProfile(userCredential, signupRequest);
    }

    public String login(LoginRequest loginRequest) {
        String email = loginRequest.getEmail();
        String password = loginRequest.getPassword();

        UserCredential userCredential = userCredentialRepository.findByEmail(email)
                .orElseThrow(() -> new AuthenticationServiceException("You don't have an account. Let's create one"));

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, password));

        if (!authentication.isAuthenticated()) {
            throw new AuthenticationServiceException("Invalid access");
        }

        UserProfile userProfile = userProfileRepository.findUserProfileByUserCredential(userCredential);
        setActiveUser(userCredential);

        String token = jwtService.generateToken(userCredential, userProfile);
        tokenService.saveToken(token, userCredential);
        return token;
    }

    public String updatePassword(String token, ChangePasswordRequest request) {
        String email = jwtService.extractEmail(token);
        UserCredential userCredential = userCredentialRepository.findByEmail(email)
                .orElseThrow(() -> new AuthenticationServiceException("User not found"));

        validatePasswordChange(request, userCredential);

        userCredential.setPassword(passwordEncoder.encode(request.getConfirmNewPassword()));
        userCredentialRepository.save(userCredential);

        return "Password changed successfully";
    }

    // === Helpers ===

    private void validateSignupEmail(String email) {
        if (userCredentialRepository.findByEmail(email).isPresent()) {
            throw new ValidationException("You are already registered in NUTRIA");
        }
    }

    private void validatePasswordChange(ChangePasswordRequest request, UserCredential user) {
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new ValidationException("Incorrect password");
        }
        if (!request.getNewPassword().equals(request.getConfirmNewPassword())) {
            throw new ValidationException("New password and confirmation do not match");
        }
    }

    private UserCredential createUserCredential(SignupRequest req) {
        return UserCredential.builder()
                .email(req.getEmail())
                .password(passwordEncoder.encode(req.getPassword()))
                .name(req.getName())
                .lastName(req.getLastName())
                .status(UserCredential.UserStatus.ACTIVE.getCode())
                .build();
    }

    private void setActiveUser(UserCredential user) {
        setUserStatus(user, UserCredential.UserStatus.ACTIVE);
    }

    private void setInactiveUser(UserCredential user) {
        setUserStatus(user, UserCredential.UserStatus.INACTIVE);
    }

    private void setUserStatus(UserCredential user, UserCredential.UserStatus status) {
        user.setStatus(status.getCode());
        userCredentialRepository.save(user);
    }
}
