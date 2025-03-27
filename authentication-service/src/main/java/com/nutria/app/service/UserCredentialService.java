package com.nutria.app.service;

import com.nutria.app.dto.LoginRequest;
import com.nutria.app.dto.SignupRequest;
import com.nutria.app.model.UserCredential;
import com.nutria.app.repository.UserCredentialRepository;
import com.thoughtworks.xstream.converters.reflection.ObjectAccessException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserCredentialService {

    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final UserCredentialRepository userCredentialRepository;
    private final AuthenticationManager authenticationManager;


    public UserCredential signup(SignupRequest signupRequest) {

        String email = signupRequest.getEmail();
        String password = signupRequest.getPassword();

        if (userCredentialRepository.findByEmail(email).isPresent()) {
            throw new ObjectAccessException("You are already registered in NUTRIA");
        }

        UserCredential userCredential = UserCredential.builder()
                .email(email)
                .password(passwordEncoder.encode(password))
                .name(signupRequest.getName())
                .lastName(signupRequest.getLastName())
                .status(UserCredential.UserStatus.ACTIVE)
                .build();

        return userCredentialRepository.save(userCredential);
    }

    public String login(LoginRequest loginRequest) {
        String email = loginRequest.getEmail();
        String password = loginRequest.getPassword();
        UserCredential userCredential = userCredentialRepository.findByEmail(email).orElseThrow(()->new AuthenticationServiceException("You don't have an account. Let's create one"));

        Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, password));

        if (authentication.isAuthenticated()) {
            updateUserStatus(userCredential);
            return jwtService.generateToken(userCredential.getId(), email);
        } else {
            throw new AuthenticationServiceException("Invalid access");
        }
    }


    private void updateUserStatus(UserCredential userCredential) {
        userCredential.setStatus(UserCredential.UserStatus.ACTIVE);
        userCredentialRepository.save(userCredential);

    }
}
