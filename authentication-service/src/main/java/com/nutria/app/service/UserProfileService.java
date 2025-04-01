package com.nutria.app.service;

import com.nutria.app.dto.SignupRequest;
import com.nutria.app.dto.SuggestedGoal;
import com.nutria.app.model.UserCredential;
import com.nutria.app.model.UserProfile;
import com.nutria.app.repository.UserProfileRepository;
import com.nutria.common.exceptions.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserProfileRepository userProfileRepository;

    public double calculateBmr(SignupRequest signupRequest) {
        int age = signupRequest.getAge();
        int gender = UserProfile.Gender.valueOf(signupRequest.getGender()).getValue();
        double height = signupRequest.getHeight();
        double weight = signupRequest.getWeight();

        return ((10 * weight) + (6.25 * height) - (5 * age) + gender);
    }

    public double calculateBmi(SignupRequest signupRequest) {
        double height = signupRequest.getHeight();
        double weight = signupRequest.getWeight();
        double m = height / 100;
        double i = Math.pow(m, 2);
        return weight / i;
    }

    public double calculateTdee(double bmr, SignupRequest signupRequest) {
        return bmr * UserProfile.ActivityLevel.valueOf(signupRequest.getActivityLevel()).getValue();
    }

    public SuggestedGoal suggestedGoal(SignupRequest signupRequest) {

        double bmiMin = UserProfile.BmiBalance.MAINTAIN.getMinBmi();
        double bmiMax = UserProfile.BmiBalance.MAINTAIN.getMaxBmi();
        double bmi = calculateBmi(signupRequest);

        SuggestedGoal suggestedGoal = new SuggestedGoal();

        for (UserProfile.BmiBalance bmiBalance : UserProfile.BmiBalance.values()) {
            if (bmiBalance.isWithinRange(bmi)) {
                suggestedGoal.setSuggestedGoal(bmiBalance.getDescription());
                break;
            }
        }

        double height = signupRequest.getHeight() / 100;
        suggestedGoal.setMinWeight(bmiMin * height * height);
        suggestedGoal.setMaxWeight(bmiMax * height * height);

        return suggestedGoal;
    }

    public UserProfile saveUserProfile(UserCredential userCredential, SignupRequest signupRequest) {
        try {
            double bmr = calculateBmr(signupRequest);

            UserProfile userProfile = UserProfile.builder()
                    .userCredential(userCredential)
                    .userName(signupRequest.getName())
                    .userLastname(signupRequest.getLastName())
                    .age(signupRequest.getAge())
                    .gender(UserProfile.Gender.valueOf(signupRequest.getGender()).getCode())
                    .height(signupRequest.getHeight())
                    .weight(signupRequest.getWeight())
                    .weightGoal(signupRequest.getWeightGoal())
                    .activityLevel(UserProfile.ActivityLevel.valueOf(signupRequest.getActivityLevel()).getValue())
                    .bmr(bmr)
                    .bmi(calculateBmi(signupRequest))
                    .tdee(calculateTdee(bmr, signupRequest))
                    .caloricAdjustment(UserProfile.CaloricAdjustment.valueOf(signupRequest.getCaloricAdjustment()).getValue())
                    .build();

            userProfileRepository.save(userProfile);

            return userProfile;

        } catch (Exception e) {
            throw new ValidationException(e.getMessage());
        }
    }


}
