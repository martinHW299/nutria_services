package com.nutria.app.service;


import com.nutria.app.model.WeightTrace;
import com.nutria.app.repository.WeightTraceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WeightTraceService {

    private final JwtService jwtService;
    private final WeightTraceRepository weightTraceRepository;

    private double weightChangeByIntake(double tdee, Long period, double calories) {
        double periodicTdee = tdee * period;
        double balance = periodicTdee - calories;
        return balance / 7700;
    }

    public WeightTrace saveWeightTrace(String token, Long period, double caloriesConsumed) {

        Long userId = jwtService.extractId(token);
        Double tdee = jwtService.extractTdee(token);

        double weightChange = weightChangeByIntake(tdee, period, caloriesConsumed);


        WeightTrace weightTrace = WeightTrace.builder()
                .userId(userId)
                .traceWeight(weightChange)
                .build();

        return weightTraceRepository.save(weightTrace);
    }

}
