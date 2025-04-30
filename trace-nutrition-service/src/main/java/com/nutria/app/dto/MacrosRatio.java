package com.nutria.app.dto;


import lombok.Getter;

@Getter
public enum MacrosRatio {
    LOSS(0.40, 0.30, 0.30),  // Weight Loss: 40% fat, 30% protein, 30% carbs
    GAIN(0.30, 0.40, 0.30),  // Muscle Gain: 30% fat, 40% protein, 30% carbs
    MAINTAIN(0.30, 0.30, 0.40);  // Maintenance: 30% fat, 30% protein, 40% carbs

    private final double fatPercentage;
    private final double proteinPercentage;
    private final double carbPercentage;

    MacrosRatio(double fatPercentage, double proteinPercentage, double carbPercentage) {
        this.fatPercentage = fatPercentage;
        this.proteinPercentage = proteinPercentage;
        this.carbPercentage = carbPercentage;
    }
}