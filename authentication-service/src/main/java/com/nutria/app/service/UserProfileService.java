package com.nutria.app.service;

import com.nutria.app.dto.SignupRequest;
import com.nutria.app.dto.SuggestedGoal;
import com.nutria.app.model.UserCredential;
import com.nutria.app.model.UserProfile;
import com.nutria.app.repository.UserProfileRepository;
import com.nutria.app.utility.InputValidator;
import com.nutria.common.exceptions.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;


@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserProfileRepository userProfileRepository;
    private final InputValidator inputValidator;

    public UserProfile saveUserProfile(UserCredential userCredential, SignupRequest signupRequest) {
        try {
            inputValidator.validateSignUpRequest(signupRequest);

            double bmr = calculateBmr(signupRequest);
            double bmi = calculateBmi(signupRequest);
            double tdee = calculateTdeeAdjusted(bmr, signupRequest);
            int genderCode = getGenderCode(signupRequest.getGender());
            double caloricAdjustment = getCaloricAdjustment(signupRequest.getCaloricAdjustment());
            double activityLevel = getActivityLevel(signupRequest.getActivityLevel());

            UserProfile userProfile = UserProfile.builder()
                    .userCredential(userCredential)
                    .userName(signupRequest.getName())
                    .userLastname(signupRequest.getLastName())
                    .age(signupRequest.getAge())
                    .gender(signupRequest.getGender())
                    .height(signupRequest.getHeight())
                    .weight(signupRequest.getWeight())
                    .weightGoal(signupRequest.getWeightGoal())
                    .activityLevel(activityLevel)
                    .bmr(bmr)
                    .bmi(bmi)
                    .tdee(tdee)
                    .caloricAdjustment(caloricAdjustment)
                    .build();

            return userProfileRepository.save(userProfile);

        } catch (Exception e) {
            throw new ValidationException("Error saving user profile: " + e.getMessage());
        }
    }

    // === CALCULATIONS ===

    public double calculateBmr(SignupRequest req) {
        int gen = getGenderCode(req.getGender());
        return (10 * req.getWeight()) + (6.25 * req.getHeight()) - (5 * req.getAge()) + gen;
    }

    public double calculateBmi(SignupRequest req) {
        return calculateBmi(req.getHeight(), req.getWeight());
    }

    public double calculateBmi(double height, double weight) {
        return weight / Math.pow(height / 100, 2);
    }

    public double calculateTdee(double bmr, String activityLevel) {
        return bmr * getActivityLevel(activityLevel);
    }

    public double calculateTdeeAdjusted(double bmr, SignupRequest req) {
        double baseTdee = calculateTdee(bmr, req.getActivityLevel());
        double adjustment = getCaloricAdjustment(req.getCaloricAdjustment());
        return baseTdee + (baseTdee * adjustment);
    }

    public SuggestedGoal suggestedGoal(double height, double weight) {
        double bmi = calculateBmi(height, weight);
        SuggestedGoal suggestedGoal = new SuggestedGoal();

        for (UserProfile.BmiBalance bmiBalance : UserProfile.BmiBalance.values()) {
            if (bmiBalance.isWithinRange(bmi)) {
                suggestedGoal.setSuggestedGoal(bmiBalance.getDescription());
                break;
            }
        }

        double heightM = height / 100;
        suggestedGoal.setMinWeight(UserProfile.BmiBalance.MAINTAIN.getMinBmi() * heightM * heightM);
        suggestedGoal.setMaxWeight(UserProfile.BmiBalance.MAINTAIN.getMaxBmi() * heightM * heightM);

        return suggestedGoal;
    }

    // === ENUM HELPERS ===
    private int getGenderCode(String gender) {
        return UserProfile.Gender.valueOf(gender).getValue();
    }

    private double getActivityLevel(String level) {
        return UserProfile.ActivityLevel.valueOf(level).getValue();
    }

    private double getCaloricAdjustment(String adjustment) {
        return UserProfile.CaloricAdjustment.valueOf(adjustment).getValue();
    }
}

