package com.nutria.app.utility;


import com.nutria.app.dto.SignupRequest;
import com.nutria.app.model.UserProfile;
import org.springframework.stereotype.Component;

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
}
