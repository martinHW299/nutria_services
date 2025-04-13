package com.nutria.app.utility;


import com.nutria.app.dto.SignupRequest;
import com.nutria.app.model.UserProfile;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class InputValidator {

    public void validateSignUpRequest(SignupRequest signupRequest) {
        if (signupRequest == null) {
            throw new IllegalArgumentException("Signup request cannot be null.");
        }

        if (isBlank(signupRequest.getEmail()) || isBlank(signupRequest.getPassword())) {
            throw new IllegalArgumentException("Email and password cannot be empty.");
        }

        if (isBlank(signupRequest.getName()) || isBlank(signupRequest.getLastName())) {
            throw new IllegalArgumentException("Name and last name cannot be empty.");
        }

        Integer age = signupRequest.getAge();
        if (age == null || age <= 0 || age > 120) {
            throw new IllegalArgumentException("Age must be between 1 and 120.");
        }

        Double height = signupRequest.getHeight();
        if (height == null || height <= 0) {
            throw new IllegalArgumentException("Height must be a positive number.");
        }

        Double weight = signupRequest.getWeight();
        if (weight == null || weight <= 0) {
            throw new IllegalArgumentException("Weight must be a positive number.");
        }

        Double weightGoal = signupRequest.getWeightGoal();
        if (weightGoal == null || weightGoal <= 0) {
            throw new IllegalArgumentException("Weight goal must be a positive number.");
        }

        validateEnum(signupRequest.getGender(), UserProfile.Gender.class, "gender");
        validateEnum(signupRequest.getActivityLevel(), UserProfile.ActivityLevel.class, "activity level");
        validateEnum(signupRequest.getCaloricAdjustment(), UserProfile.CaloricAdjustment.class, "caloric adjustment");
    }

    public void validateAdvisorInput(Double height, Double weight) {
        if ( height == null || weight == null || height <= 0|| weight <= 0) {
            throw new IllegalArgumentException("Height / Weight values not found.");
        }
    }

    private boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }

    private <T extends Enum<T>> void validateEnum(String value, Class<T> enumClass, String fieldName) {
        try {
            Enum.valueOf(enumClass, value);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid " + fieldName + " value: " + value);
        }
    }

    public void emailInputValidator(String email) {
        String EMAIL_PATTERN = "^[a-zA-Z0-9_!#$%&'*+/=?`{|}~^.-]+@[a-zA-Z0-9.-]+$";
        Pattern pattern = Pattern.compile(EMAIL_PATTERN);

        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be empty");
        }

        Matcher matcher = pattern.matcher(email);
        if (!matcher.matches()){
            throw new IllegalArgumentException("Invalid password. Try with another mail address");
        }
    }

    public void passwordInputValidator(String password) {
        int MIN_LENGTH = 8;
        int MAX_LENGTH = 128;

        if (password == null) {
            throw new IllegalArgumentException("Password cannot be empty");
        }

        if (password.length() < MIN_LENGTH ) {
            throw new IllegalArgumentException("Password is too short");
        }

        if (password.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("Password is too long");
        }

        // Check for at least one uppercase letter
        if (!password.matches(".*[A-Z].*")) {
            throw new IllegalArgumentException("Password must contain at least one uppercase letter");
        }

        // Check for at least one lowercase letter
        if (!password.matches(".*[a-z].*")) {
            throw new IllegalArgumentException("Password must contain at least one lowercase letter");
        }

        // Check for at least one digit
        if (!password.matches(".*\\d.*")) {
            throw new IllegalArgumentException("Password must contain at least one digit");
        }

        // Check for at least one special character
        if (!password.matches(".*[!@#$%^&*()\\-_=+\\\\|\\[{\\]};:'\",<.>/?].*")) {
            throw new IllegalArgumentException("Password must contain at least one special character");
        }
    }
}
