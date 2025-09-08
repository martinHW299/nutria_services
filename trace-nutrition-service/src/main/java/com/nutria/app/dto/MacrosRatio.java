package com.nutria.app.dto;


import lombok.Getter;

@Getter
public enum MacrosRatio {
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