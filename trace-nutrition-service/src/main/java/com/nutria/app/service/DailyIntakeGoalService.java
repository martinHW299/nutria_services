package com.nutria.app.service;


import com.nutria.app.model.DailyIntakeGoal;
import com.nutria.app.repository.DailyIntakeGoalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class DailyIntakeGoalService {

    private final JwtService jwtService;
    private final DailyIntakeGoalRepository dailyIntakeGoalRepository;

    private double getCaloricIntakeGoal(double tdee, double weightGoal) {
        return tdee + (tdee * weightGoal);
    }

    private double getProteinIntakeGoal(double tdee) {
        return (tdee * 0.2)/4; //g
    }

    private double getCarbsIntakeGoal(double tdee) {
        return (tdee * 0.55)/4; //g
    }

    private double getFatsIntakeGoal(double tdee) {
        return (tdee * 0.25)/9; //g
    }

    public DailyIntakeGoal saveDailyIntakeGoal(String token) {
        Long userId = jwtService.extractId(token);
        double tdee = jwtService.extractWeightTdee(token);
        double weightGoal = jwtService.extractWeightGoal(token);

        DailyIntakeGoal dailyIntakeGoal = DailyIntakeGoal.builder()
                .userId(userId)
                .calories(getCaloricIntakeGoal(tdee, weightGoal))
                .proteins(tdee)
                .carbs(tdee)
                .fats(tdee)
                .weightGoal(weightGoal)
                .build();

        return dailyIntakeGoalRepository.save(dailyIntakeGoal);
    }

}
