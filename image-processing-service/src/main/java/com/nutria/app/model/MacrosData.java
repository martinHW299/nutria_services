package com.nutria.app.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "macros_data")
public class MacrosData {

    @Id
    private String id;

    private Long userId;
    private String description;
    private Double calories;
    private Double proteins;
    private Double carbs;
    private Double fats;
    private Double servingSize;
    private String status;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    /**
     * Constructor for creating new MacrosData from AI analysis
     */
    public MacrosData(String description, Double calories, Double proteins,
                      Double carbs, Double fats, Double servingSize) {
        this.description = description;
        this.calories = calories;
        this.proteins = proteins;
        this.carbs = carbs;
        this.fats = fats;
        this.servingSize = servingSize;
        this.status = MacrosStatus.ACTIVE.getCode();
    }

    /**
     * Validates that all nutritional values are non-negative
     */
    public void validateNutritionalValues() {
        if (calories != null && calories < 0) {
            throw new IllegalArgumentException("Calories cannot be negative");
        }
        if (proteins != null && proteins < 0) {
            throw new IllegalArgumentException("Proteins cannot be negative");
        }
        if (carbs != null && carbs < 0) {
            throw new IllegalArgumentException("Carbohydrates cannot be negative");
        }
        if (fats != null && fats < 0) {
            throw new IllegalArgumentException("Fats cannot be negative");
        }
        if (servingSize != null && servingSize <= 0) {
            throw new IllegalArgumentException("Serving size must be greater than 0");
        }
    }

    /**
     * Safely gets calories with default value
     */
    public Double getCaloriesOrDefault(Double defaultValue) {
        return calories != null ? calories : defaultValue;
    }

    /**
     * Safely gets proteins with default value
     */
    public Double getProteinsOrDefault(Double defaultValue) {
        return proteins != null ? proteins : defaultValue;
    }

    /**
     * Safely gets carbs with default value
     */
    public Double getCarbsOrDefault(Double defaultValue) {
        return carbs != null ? carbs : defaultValue;
    }

    /**
     * Safely gets fats with default value
     */
    public Double getFatsOrDefault(Double defaultValue) {
        return fats != null ? fats : defaultValue;
    }

    /**
     * Safely gets serving size with default value
     */
    public Double getServingSizeOrDefault(Double defaultValue) {
        return servingSize != null ? servingSize : defaultValue;
    }

    /**
     * Calculates total macronutrients in grams
     */
    public Double getTotalMacrosInGrams() {
        return getProteinsOrDefault(0.0) + getCarbsOrDefault(0.0) + getFatsOrDefault(0.0);
    }

    /**
     * Calculates calories from macronutrients (4-4-9 rule)
     */
    public Double getCalculatedCalories() {
        return (getProteinsOrDefault(0.0) * 4) +
                (getCarbsOrDefault(0.0) * 4) +
                (getFatsOrDefault(0.0) * 9);
    }

    /**
     * Checks if this macro data is active
     */
    public boolean isActive() {
        return MacrosStatus.ACTIVE.getCode().equals(status);
    }

    /**
     * Marks this macro data as inactive
     */
    public void markAsInactive() {
        this.status = MacrosStatus.INACTIVE.getCode();
    }

    /**
     * Marks this macro data as active
     */
    public void markAsActive() {
        this.status = MacrosStatus.ACTIVE.getCode();
    }

    @Getter
    public enum MacrosStatus {
        ACTIVE("AC"),
        INACTIVE("IN");

        private final String code;

        MacrosStatus(String code) {
            this.code = code;
        }

        /**
         * Get status enum from code
         */
        public static MacrosStatus fromCode(String code) {
            for (MacrosStatus status : values()) {
                if (status.getCode().equals(code)) {
                    return status;
                }
            }
            throw new IllegalArgumentException("Unknown status code: " + code);
        }
    }
}