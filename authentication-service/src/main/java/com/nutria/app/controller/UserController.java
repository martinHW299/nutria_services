package com.nutria.app.controller;


import com.nutria.app.dto.ChangePasswordRequest;
import com.nutria.app.dto.LoginRequest;
import com.nutria.app.dto.SignupRequest;
import com.nutria.app.dto.SuggestedGoal;
import com.nutria.app.model.UserProfile;
import com.nutria.app.service.TokenService;
import com.nutria.app.service.UserProfileService;
import com.nutria.app.service.UserService;
import com.nutria.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class UserController {

    private final UserService userService;
    private final UserProfileService userProfileService;
    private final TokenService tokenService;

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<UserProfile>> register(@RequestBody SignupRequest signupRequest) {
        return ResponseEntity.ok(ApiResponse.success(userService.signup(signupRequest)));
    }

    @GetMapping("/login")
    public ResponseEntity<ApiResponse<String>> login(@RequestBody LoginRequest loginRequest) {
        return ResponseEntity.ok(ApiResponse.success(userService.login(loginRequest)));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(@RequestHeader(HttpHeaders.AUTHORIZATION) String token) {
        tokenService.revokeToken(token.substring(7).trim());
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully"));
    }

    @PostMapping("/validate")
    public ResponseEntity<Boolean> validateToken(@RequestBody Map<String, String> payload) {
        boolean isValid = tokenService.isTokenValid(payload.get("token"));
        return ResponseEntity.ok(isValid);
    }

    @PostMapping("/update-password")
    public ResponseEntity<ApiResponse<String>> changePassword(@RequestHeader(HttpHeaders.AUTHORIZATION) String token, @RequestBody ChangePasswordRequest changePasswordRequest) {
        return ResponseEntity.ok(ApiResponse.success(userService.updatePassword(token, changePasswordRequest)));
    }

    @GetMapping("/advisor")
    public SuggestedGoal suggestedGoal(@RequestBody Map<Double, Double> payload) {
        return userProfileService.suggestedGoal(payload.get("height"), payload.get("weight"));
    }



}
