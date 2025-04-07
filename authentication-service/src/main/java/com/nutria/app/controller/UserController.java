package com.nutria.app.controller;


import com.nutria.app.dto.LoginRequest;
import com.nutria.app.dto.SignupRequest;
import com.nutria.app.model.UserProfile;
import com.nutria.app.service.TokenService;
import com.nutria.app.service.UserService;
import com.nutria.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class UserController {

    private final UserService userService;
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
    public ResponseEntity<ApiResponse<String>> logout(@RequestHeader("Authorization") String token) {
        String t = token.substring(7);
        tokenService.revokeToken(t);
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully"));
    }

    @PostMapping("/validate")
    public ResponseEntity<Boolean> validateToken(@RequestHeader("Authorization") String token) {
        String t = token.substring(7);
        boolean isValid = tokenService.isTokenValid(t);
        return ResponseEntity.ok(isValid);
    }



}
