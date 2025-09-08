package com.nutria.app.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * DTO for advanced AI food analysis requests
 * Extends the basic image processing with AI-specific parameters
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdvancedAnalysisRequest {

    private String image;
    private boolean useMockData = false;
    private Double userServingSize;
    private double temperature = 0.7;

    /**
     * Constructor for quick creation with just image and mock data flag
     */
    public AdvancedAnalysisRequest(String image, boolean useMockData) {
        this.image = image;
        this.useMockData = useMockData;
    }

    /**
     * Constructor for creation with custom serving size
     */
    public AdvancedAnalysisRequest(String image, Double userServingSize, double temperature) {
        this.image = image;
        this.useMockData = false;
        this.userServingSize = userServingSize;
        this.temperature = temperature;
    }
}