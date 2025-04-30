package com.nutria.app.utilities;


import org.springframework.stereotype.Component;

@Component
public class InputValidator {
    public static double roundUpToTwoDecimals(double value) {
        return Math.ceil(value * 100) / 100.0;
    }
}
