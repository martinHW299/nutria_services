package com.nutria.app.service;

import com.nutria.app.dto.SignupRequest;
import com.nutria.app.dto.SuggestedGoal;
import com.nutria.app.model.UserCredential;
import com.nutria.app.model.UserProfile;
import com.nutria.app.repository.UserCredentialRepository;
import com.nutria.app.repository.UserProfileRepository;
import com.nutria.common.exceptions.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.stereotype.Service;

import static com.nutria.app.utility.InputValidator.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserProfileService {

    private static final double KCAL_PER_KG = 7700.0;
    private static final double MIN_SAFE_KCAL = 1200.0;

    private final UserProfileRepository userProfileRepository;
    private final UserCredentialRepository userCredentialRepository;
    private final JwtService jwtService;

    public UserProfile saveUserProfile(UserCredential userCredential, SignupRequest signupRequest) {
        try {
            // Base calculations
            double bmr = calculateBmr(signupRequest);
            double bmi = calculateBmi(signupRequest);
            double tdee = calculateTdee(bmr, signupRequest.getActivityLevel());

            // New: add/subtract kcal/day based on rate (kg/week)
            double kcalGoal = calculateDailyKcalGoal(signupRequest, tdee);

            UserProfile userProfile = UserProfile.builder()
                    .userCredential(userCredential)
                    .userName(signupRequest.getName())
                    .userLastname(signupRequest.getLastName())
                    .age(signupRequest.getAge())
                    .gender(signupRequest.getGender())
                    .height(signupRequest.getHeight())
                    .weight(signupRequest.getWeight())
                    .weightGoal(signupRequest.getWeightGoal())
                    .activityLevel(getActivityLevel(signupRequest.getActivityLevel()))
                    .bmr(bmr)
                    .bmi(bmi)
                    .tdee(tdee)
                    .caloricAdjustment(signupRequest.getCaloricAdjustment())
                    .build();

            log.info("Calculated plan -> BMR: {}, TDEE: {}, kcalGoal: {}",
                    bmr, tdee, kcalGoal);

            return userProfileRepository.save(userProfile);

        } catch (Exception e) {
            throw new ValidationException("Error saving user profile: " + e.getMessage());
        }
    }

    // === Existing methods ===
    public double calculateBmr(SignupRequest req) {
        int gen = getGenderCode(req.getGender());
        return roundUpToTwoDecimals((10 * req.getWeight()) + (6.25 * req.getHeight()) - (5 * req.getAge()) + gen);
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

    // Keep for backwards compatibility if some callers still use % of TDEE
    public double calculateTdeeAdjusted(double bmr, SignupRequest req) {
        double baseTdee = calculateTdee(bmr, req.getActivityLevel());
        double percent = getCaloricAdjustment(req.getCaloricAdjustment()); // if your enum is %-based
        return roundUpToTwoDecimals(baseTdee * (1 + percent));
    }

    // === New: daily kcal goal by additive kcal/day delta (recommended) ===
    public double calculateDailyKcalGoal(SignupRequest req, double tdee) {
        double dailyDelta = dailyCalorieDeltaFromAdjustment(req.getCaloricAdjustment());
        double kcalGoal = Math.max(MIN_SAFE_KCAL, tdee + dailyDelta);
        return roundUpToTwoDecimals(kcalGoal);
    }

    /**
     * Translate CaloricAdjustment into kcal/day delta using kg/week.
     * Example mapping:
     *  LOSE_025 -> -(7700 * 0.25)/7  ≈ -275
     *  LOSE_050 -> -(7700 * 0.50)/7  ≈ -550
     *  LOSE_100 -> -(7700 * 1.00)/7  ≈ -1100
     *  MAINTAIN -> 0
     *  GAIN_025 ->  (7700 * 0.25)/7  ≈ +275
     *  GAIN_050 ->  (7700 * 0.50)/7  ≈ +550
     *
     * Fallback: if your enum currently returns a raw number via getValue(),
     * we'll treat it as kg/week (positive gain, negative loss).
     */
    private double dailyCalorieDeltaFromAdjustment(String adjustment) {
        UserProfile.CaloricAdjustment adj = UserProfile.CaloricAdjustment.valueOf(adjustment);
        // Treat enum value as kg/week if not one of the known names
        return switch (adj.name()) {
            case "LOSE_025" -> -(KCAL_PER_KG * 0.25) / 7.0;
            case "LOSE_050" -> -(KCAL_PER_KG * 0.50) / 7.0;
            case "LOSE_100" -> -(KCAL_PER_KG) / 7.0;
            case "MAINTAIN" -> 0.0;
            case "GAIN_025" -> +(KCAL_PER_KG * 0.25) / 7.0;
            case "GAIN_050" -> +(KCAL_PER_KG * 0.50) / 7.0;
            default -> {
                double kgPerWeek = getCaloricAdjustment(adjustment);
                yield (KCAL_PER_KG * kgPerWeek) / 7.0;
            }
        };
    }

    // === ENUM HELPERS ===
    private int getGenderCode(String gender) {
        return UserProfile.Gender.valueOf(gender).getValue();
    }

    private double getActivityLevel(String level) {
        return UserProfile.ActivityLevel.valueOf(level).getValue();
    }

    /**
     * IMPORTANT:
     * If your CaloricAdjustment enum is reworked to store kg/week (recommended),
     * then getValue() should return kg/week (positive gain, negative loss).
     */
    private double getCaloricAdjustment(String adjustment) {
        return UserProfile.CaloricAdjustment.valueOf(adjustment).getValue();
    }

    public UserProfile getUserProfile(String token) {
        String email = jwtService.extractEmail(token);
        UserCredential userCredential = userCredentialRepository.findByEmail(email)
                .orElseThrow(() -> new AuthenticationServiceException("Profile of the user not found"));
        return userProfileRepository.findUserProfileByUserCredential(userCredential)
                .orElseThrow(() -> new AuthenticationServiceException("User " + userCredential.getEmail() + " does not have a profile"));
    }

    // Advisor method unchanged
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
}
