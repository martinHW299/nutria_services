package com.nutria.app.service;

import com.nutria.app.dto.ChangePasswordRequest;
import com.nutria.app.dto.LoginRequest;
import com.nutria.app.dto.SignupRequest;
import com.nutria.app.model.UserCredential;
import com.nutria.app.model.UserProfile;
import com.nutria.app.repository.UserCredentialRepository;
import com.nutria.app.repository.UserProfileRepository;
import com.nutria.app.utility.InputValidator;
import com.nutria.common.exceptions.ResourceNotFoundException;
import com.nutria.common.exceptions.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import static com.nutria.app.utility.InputValidator.emailInputValidator;
import static com.nutria.app.utility.InputValidator.passwordInputValidator;


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

        emailInputValidator(signupRequest.getEmail());
        passwordInputValidator(signupRequest.getPassword());

        UserCredential userCredential = createUserCredential(signupRequest);
        userCredentialRepository.save(userCredential);

        return userProfileService.saveUserProfile(userCredential, signupRequest);
    }

    private UserCredential createUserCredential(SignupRequest req) {
        return UserCredential.builder().email(req.getEmail())
                .password(passwordEncoder.encode(req.getPassword()))
                .name(req.getName())
                .lastName(req.getLastName())
                .status(UserCredential.UserStatus.ACTIVE.getCode())
                .build();
    }

    public String login(LoginRequest loginRequest) {

        emailInputValidator(loginRequest.getEmail());
        passwordInputValidator(loginRequest.getPassword());

        String email = loginRequest.getEmail();
        String password = loginRequest.getPassword();

        UserCredential userCredential = userCredentialRepository.findByEmail(email).orElseThrow(() -> new AuthenticationServiceException("You don't have an account."));

        Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, password));

        if (!authentication.isAuthenticated()) {
            throw new AuthenticationServiceException("Invalid access.");
        }

        UserProfile userProfile = userProfileRepository.findUserProfileByUserCredential(userCredential).orElseThrow(() -> new ResourceNotFoundException("User not found."));
        setActiveUser(userCredential);

        String token = jwtService.generateToken(userCredential, userProfile);
        tokenService.saveToken(token, userCredential);
        return token;
    }

    public String updatePassword(String token, ChangePasswordRequest request) {
        passwordInputValidator(request.getNewPassword());

        String email = jwtService.extractEmail(token);
        UserCredential userCredential = userCredentialRepository.findByEmail(email).orElseThrow(() -> new AuthenticationServiceException("User not found."));

        validatePasswordChange(request, userCredential);

        userCredential.setPassword(passwordEncoder.encode(request.getConfirmNewPassword()));
        userCredentialRepository.save(userCredential);

        return "Password changed successfully.";
    }

    // === Helpers ===

    private void validateSignupEmail(String email) {
        if (userCredentialRepository.findByEmail(email).isPresent()) {
            throw new ValidationException("You are already registered in the system.");
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

    private void setActiveUser(UserCredential user) {
        setUserStatus(user, UserCredential.UserStatus.ACTIVE);
    }

    public void setInactiveUser(String token) {
        String email = jwtService.extractEmail(token);
        UserCredential user = userCredentialRepository.findByEmail(email).orElseThrow(() -> new AuthenticationServiceException("User not found"));
        tokenService.revokeToken(token);
        setUserStatus(user, UserCredential.UserStatus.INACTIVE);
    }

    private void setUserStatus(UserCredential user, UserCredential.UserStatus status) {
        user.setStatus(status.getCode());
        userCredentialRepository.save(user);
    }
}
