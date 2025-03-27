package com.nutria.app.controller;


import com.nutria.app.dto.LoginRequest;
import com.nutria.app.dto.SignupRequest;
import com.nutria.app.model.UserCredential;
import com.nutria.app.service.UserCredentialService;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class UserCredentialController {

    private final UserCredentialService userCredentialService;

    @PostMapping("/signup")
    public ResponseEntity<UserCredential> register(@RequestBody SignupRequest signupRequest) {
        return ResponseEntity.ok(userCredentialService.signup(signupRequest));
    }

    @GetMapping("/login")
    public ResponseEntity<String> login(@RequestBody LoginRequest loginRequest) {
        return ResponseEntity.ok(userCredentialService.login(loginRequest));
    }



}
