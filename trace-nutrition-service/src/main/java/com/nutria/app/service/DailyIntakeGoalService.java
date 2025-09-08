package com.nutria.app.service;

import com.nutria.app.dto.MacrosGoal;
import com.nutria.app.model.DailyIntakeGoal;
import com.nutria.app.repository.DailyIntakeGoalRepository;
import com.nutria.common.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Date;

import static com.nutria.app.utilities.InputValidator.roundUpToTwoDecimals;

@Slf4j
@Service
@RequiredArgsConstructor
public class DailyIntakeGoalService {

    private final JwtService jwtService;
    private final DailyIntakeGoalRepository dailyIntakeGoalRepository;

    /**
     * Fixed ratio: 20% protein, 50% carbs, 30% fats
     */
    private MacrosGoal calculateMacrosGoal(double dailyCalories) {
        double protein = roundUpToTwoDecimals((dailyCalories * 0.20) / 4.0);
        double carbs   = roundUpToTwoDecimals((dailyCalories * 0.50) / 4.0);
        double fats    = roundUpToTwoDecimals((dailyCalories * 0.30) / 9.0);
        return new MacrosGoal(protein, carbs, fats);
    }

    public DailyIntakeGoal saveDailyIntakeGoal(String token, Date date) {
        long userId     = jwtService.extractId(token);
        double tdee     = jwtService.extractTdee(token);   // if you later switch to kcalGoal, replace here
        double weight   = jwtService.extractWeight(token);
        double weightGoal = jwtService.extractWeightGoal(token);

        // Always use the same 50/20/30 split
        MacrosGoal macrosGoal = calculateMacrosGoal(tdee);

        DailyIntakeGoal dailyIntakeGoal = DailyIntakeGoal.builder()
                .userId(userId)
                .calories(tdee)
                .proteins(macrosGoal.getProtein())
                .carbs(macrosGoal.getCarbs())
                .fats(macrosGoal.getFats())
                .weightGoal(weightGoal)
                .recordAt(date)
                .build();

        return dailyIntakeGoalRepository.save(dailyIntakeGoal);
    }

    public DailyIntakeGoal getDailyIntakeGoal(String token, Date targetDate) {
        Long userId = jwtService.extractId(token);
        return dailyIntakeGoalRepository.getDailyIntakeGoalByUserId(userId, targetDate)
                .orElseThrow(() -> new ResourceNotFoundException("Intake goal of " + userId + " not found"));
    }
}
