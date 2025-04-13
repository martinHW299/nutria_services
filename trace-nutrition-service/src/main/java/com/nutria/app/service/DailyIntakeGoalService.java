package com.nutria.app.service;


import com.nutria.app.model.DailyIntakeGoal;
import com.nutria.app.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DailyIntakeGoalService {

    private final JwtService jwtService;
    private final DailyIntakeGoalRepository dailyIntakeGoalRepository;

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
        Double tdee = jwtService.extractTdee(token);
        Double weightGoal = jwtService.extractWeightGoal(token);

        DailyIntakeGoal dailyIntakeGoal = DailyIntakeGoal.builder()
                .userId(userId)
                .calories(tdee)
                .proteins(getProteinIntakeGoal(tdee))
                .carbs(getCarbsIntakeGoal(tdee))
                .fats(getFatsIntakeGoal(tdee))
                .weightGoal(weightGoal)
                .build();

        return dailyIntakeGoalRepository.save(dailyIntakeGoal);
    }

}
