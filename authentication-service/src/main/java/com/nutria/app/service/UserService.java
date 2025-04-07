package com.nutria.app.service;

import com.nutria.app.dto.LoginRequest;
import com.nutria.app.dto.SignupRequest;
import com.nutria.app.model.UserCredential;
import com.nutria.app.model.UserProfile;
import com.nutria.app.repository.UserCredentialRepository;
import com.nutria.app.repository.UserProfileRepository;
import com.nutria.common.exceptions.ResourceNotFoundException;
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

        String email = signupRequest.getEmail();
        String password = signupRequest.getPassword();

        if (userCredentialRepository.findByEmail(email).isPresent()) {
            throw new ValidationException("You are already registered in NUTRIA");
        }

        UserCredential userCredential = UserCredential.builder()
                .email(email)
                .password(passwordEncoder.encode(password))
                .name(signupRequest.getName())
                .lastName(signupRequest.getLastName())
                .status(UserCredential.UserStatus.ACTIVE.getCode())
                .build();

        userCredentialRepository.save(userCredential);

        return userProfileService.saveUserProfile(userCredential, signupRequest);
    }

    public String login(LoginRequest loginRequest) {
        String email = loginRequest.getEmail();
        String password = loginRequest.getPassword();
        UserCredential userCredential = userCredentialRepository.findByEmail(email).orElseThrow(()->new AuthenticationServiceException("You don't have an account. Let's create one"));
        Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, password));

        if (authentication.isAuthenticated()) {
            UserProfile userProfile = userProfileRepository.findUserProfileByUserCredential(userCredential);
            setActiveUser(userCredential);
            String token = jwtService.generateToken(userCredential, userProfile);
            tokenService.saveToken(token, userCredential);
            return token;
        } else {
            throw new AuthenticationServiceException("Invalid access");
        }
    }

    public String logout(String token) {
        UserCredential userCredential = userCredentialRepository.findByEmail(jwtService.extractEmail(token)).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        setInactiveUser(userCredential);
        return "User logged out";
    }

    private void setInactiveUser(UserCredential userCredential) {
        userCredential.setStatus(UserCredential.UserStatus.INACTIVE.getCode());
        userCredentialRepository.save(userCredential);
    }

    private void setActiveUser(UserCredential userCredential) {
        userCredential.setStatus(UserCredential.UserStatus.ACTIVE.getCode());
        userCredentialRepository.save(userCredential);
    }
}
