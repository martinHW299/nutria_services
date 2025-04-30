package com.nutria.app.service;


import com.nutria.app.model.WeightTrace;
import com.nutria.app.repository.WeightTraceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class WeightTraceService {

    private final JwtService jwtService;
    private final WeightTraceRepository weightTraceRepository;

    public double weightChangeByIntake(String token, long period, double calories) {
        double tdee = jwtService.extractTdee(token);
        double balance = calories - (tdee * period);
        return Math.round((balance / 7700.0) * 1000.0) / 1000.0;
    }

}
