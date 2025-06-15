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
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class UserController {

    private final UserService userService;
    private final UserProfileService userProfileService;
    private final TokenService tokenService;

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<UserProfile>> register(@RequestBody SignupRequest signupRequest) {
//        log.info("User signup request for email: {}", signupRequest.getEmail());
        return ResponseEntity.ok(ApiResponse.success(userService.signup(signupRequest)));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<String>> login(@RequestBody LoginRequest loginRequest) {
//        log.info("User login request for email: {}", loginRequest.getEmail());
        return ResponseEntity.ok(ApiResponse.success(userService.login(loginRequest)));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(@RequestHeader(HttpHeaders.AUTHORIZATION) String token) {
        log.info("User logout request");

        // Revoke token in database and notify gateway
        tokenService.revokeToken(token);

        // Also set user as inactive if your business logic requires it
        userService.setInactiveUser(token.substring(7).trim());

        return ResponseEntity.ok(ApiResponse.success("Logged out successfully"));
    }

    /**
     * Token validation endpoint - now primarily used for internal validation
     * The API Gateway performs its own JWT validation without calling this endpoint
     */
    @PostMapping("/validate")
    public ResponseEntity<Boolean> validateToken(@RequestHeader(HttpHeaders.AUTHORIZATION) String token) {
        boolean isValid = tokenService.isTokenValid(token);
        log.debug("Token validation request - Valid: {}", isValid);
        return ResponseEntity.ok(isValid);
    }

    /**
     * Endpoint to revoke a specific token (can be called by admin or user)
     */
    @PostMapping("/revoke-token")
    public ResponseEntity<ApiResponse<String>> revokeToken(@RequestHeader(HttpHeaders.AUTHORIZATION) String token) {
        log.info("Token revocation request");
        tokenService.revokeToken(token);
        return ResponseEntity.ok(ApiResponse.success("Token revoked successfully"));
    }

    /**
     * Endpoint to revoke all tokens for a user (useful for security incidents)
     */
    @PostMapping("/revoke-all-tokens")
    public ResponseEntity<ApiResponse<String>> revokeAllUserTokens(@RequestHeader(HttpHeaders.AUTHORIZATION) String token) {
        log.info("Revoke all tokens request");
        String result = userService.revokeAllUserTokens(token);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/update-password")
    public ResponseEntity<ApiResponse<String>> changePassword(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String token,
            @RequestBody ChangePasswordRequest changePasswordRequest) {
        log.info("Password change request");
        return ResponseEntity.ok(ApiResponse.success(userService.updatePassword(token, changePasswordRequest)));
    }

    @GetMapping("/advisor")
    public ResponseEntity<ApiResponse<SuggestedGoal>> suggestedGoal(@RequestBody Map<String, Double> payload) {
        log.debug("Suggested goal request for height: {}, weight: {}",
                payload.get("height"), payload.get("weight"));
        return ResponseEntity.ok(ApiResponse.success(
                userProfileService.suggestedGoal(payload.get("height"), payload.get("weight"))));
    }

    @GetMapping("/get-profile")
    public ResponseEntity<ApiResponse<UserProfile>> getUserProfile(@RequestHeader(HttpHeaders.AUTHORIZATION) String token) {
        log.debug("Get user profile request");
        return ResponseEntity.ok(ApiResponse.success(userProfileService.getUserProfile(token)));
    }

    /**
     * Health check endpoint for the authentication service
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> health() {
        return ResponseEntity.ok(ApiResponse.success("Authentication service is running"));
    }

    /**
     * Get token statistics for monitoring
     */
    @GetMapping("/token-stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTokenStats(@RequestHeader(HttpHeaders.AUTHORIZATION) String token) {
        log.debug("Token stats request");
        Map<String, Object> stats = userService.getTokenStatistics(token);
        return ResponseEntity.ok(ApiResponse.success(stats));
    }
}