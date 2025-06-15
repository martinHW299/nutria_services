package com.nutria.app.service;


import com.nutria.app.dto.MacrosGoal;
import com.nutria.app.dto.MacrosRatio;
import com.nutria.app.model.DailyIntakeGoal;
import com.nutria.app.repository.*;
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


    public MacrosRatio getMacronutrientRatio(double extractWeight, double extractWeightGoal) {
//        if (extractWeight < extractWeightGoal || extractWeight == extractWeightGoal) {
//            return MacrosRatio.GAIN;
//        } else if (extractWeight == extractWeightGoal) {
//            return MacrosRatio.MAINTAIN;
//        } else {
//            return MacrosRatio.LOSS;
//        }

        if (extractWeight < extractWeightGoal || extractWeight == extractWeightGoal) {
            return MacrosRatio.MAINTAIN;
//        } else if (extractWeight == extractWeightGoal) {
//            return MacrosRatio.MAINTAIN;
        } else {
            return MacrosRatio.LOSS;
        }
    }

    private MacrosGoal calculateMacrosGoal(double weight, double tdee, MacrosRatio ratio) {
        double proteinPerKg = roundUpToTwoDecimals(1.4 * weight);
        double fatsPerKg = roundUpToTwoDecimals(1.2 * weight);
//        double protein = roundUpToTwoDecimals((tdee * ratio.getProteinPercentage()) / 4); // grams
//        double carbs = roundUpToTwoDecimals((tdee * ratio.getCarbPercentage()) / 4); // grams
//        double fats = roundUpToTwoDecimals((tdee * ratio.getFatPercentage()) / 9); // grams
        double protein = proteinPerKg * 4;
        double fats = fatsPerKg * 9;
        double carbs = roundUpToTwoDecimals((tdee - protein - fats) / 4);
        return new MacrosGoal(proteinPerKg, carbs, fatsPerKg);
    }

    public DailyIntakeGoal saveDailyIntakeGoal(String token, Date date) {
        long userId = jwtService.extractId(token);
        double tdee = jwtService.extractTdee(token);
        double weight = jwtService.extractWeight(token);
        double weightGoal = jwtService.extractWeightGoal(token);
//        log.info("weight: {}, weight goal: {}", weight, weightGoal);


        MacrosRatio macrosRatio = getMacronutrientRatio(weight, weightGoal);
//        log.info("ratio: {}", macrosRatio);
        MacrosGoal macrosGoal = calculateMacrosGoal(weight, tdee, macrosRatio);

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
        return dailyIntakeGoalRepository.getDailyIntakeGoalByUserId(userId, targetDate).orElseThrow(() -> new ResourceNotFoundException("Intake goal of "+userId+" not found"));
    }
}
