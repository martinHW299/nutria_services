package com.nutria.app.service;

import com.nutria.app.dto.SignupRequest;
import com.nutria.app.dto.SuggestedGoal;
import com.nutria.app.model.UserCredential;
import com.nutria.app.model.UserProfile;
import com.nutria.app.repository.UserCredentialRepository;
import com.nutria.app.repository.UserProfileRepository;
import com.nutria.app.utility.InputValidator;
import com.nutria.common.exceptions.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static com.nutria.app.utility.InputValidator.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserProfileRepository userProfileRepository;
    private final UserCredentialRepository userCredentialRepository;
    private final JwtService jwtService;

    public UserProfile saveUserProfile(UserCredential userCredential, SignupRequest signupRequest) {
        try {
            validateSignUpRequest(signupRequest);

            double bmr = calculateBmr(signupRequest);
            double bmi = calculateBmi(signupRequest);
            double tdee = calculateTdeeAdjusted(bmr, signupRequest);
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

    public double calculateBmr(SignupRequest req) {
        int gen = getGenderCode(req.getGender());
        return roundUpToTwoDecimals(10 * req.getWeight() + 6.25 * req.getHeight() - 5 * req.getAge() + gen);
    }

    public double calculateBmi(SignupRequest req) {
        return roundUpToTwoDecimals(calculateBmi(req.getHeight(), req.getWeight()));
    }

    public double calculateBmi(double height, double weight) {
        return roundUpToTwoDecimals(weight / Math.pow(height / 100, 2));
    }

    public double calculateTdee(double bmr, String activityLevel) {
        return roundUpToTwoDecimals(bmr * getActivityLevel(activityLevel));
    }

    public double calculateTdeeAdjusted(double bmr, SignupRequest req) {
        double baseTdee = calculateTdee(bmr, req.getActivityLevel());
        double adjustment = getCaloricAdjustment(req.getCaloricAdjustment());
        return roundUpToTwoDecimals(baseTdee * (1 + adjustment));
    }

    public SuggestedGoal suggestedGoal(double height, double weight) {

        validateAdvisorInput(height, weight);

        double bmi = calculateBmi(height, weight);
        SuggestedGoal suggestedGoal = new SuggestedGoal();

        for (UserProfile.BmiBalance bmiBalance : UserProfile.BmiBalance.values()) {
            if (bmiBalance.isWithinRange(bmi)) {
                suggestedGoal.setSuggestedGoal(bmiBalance.getDescription());
                break;
            }
        }

        double heightM = height / 100;
        suggestedGoal.setMinWeight(roundUpToTwoDecimals(UserProfile.BmiBalance.MAINTAIN.getMinBmi() * heightM * heightM));
        suggestedGoal.setMaxWeight(roundUpToTwoDecimals(UserProfile.BmiBalance.MAINTAIN.getMaxBmi() * heightM * heightM));

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

    public UserProfile getUserProfile(String token) {
        String email = jwtService.extractEmail(token);
        UserCredential userCredential = userCredentialRepository.findByEmail(email).orElseThrow(() -> new AuthenticationServiceException("Profile of the user not found"));
        return userProfileRepository.findUserProfileByUserCredential(userCredential).orElseThrow(() -> new AuthenticationServiceException("User "+userCredential.getEmail()+" does not have a profile"));
    }
}

