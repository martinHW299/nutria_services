package com.nutria.app.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

/**
 * DTO for analyze and save workflow requests
 * Combines AI analysis parameters with saving requirements
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnalyzeAndSaveRequest {

    private String image;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date date;

    private boolean useMockData = false;
    private Double userServingSize;
    private double temperature = 0.7;

    /**
     * Constructor for quick creation with essential fields
     */
    public AnalyzeAndSaveRequest(String image, Date date, boolean useMockData) {
        this.image = image;
        this.date = date;
        this.useMockData = useMockData;
    }

    /**
     * Constructor for creation with custom serving size
     */
    public AnalyzeAndSaveRequest(String image, Date date, Double userServingSize, double temperature) {
        this.image = image;
        this.date = date;
        this.useMockData = false;
        this.userServingSize = userServingSize;
        this.temperature = temperature;
    }

    /**
     * Converts to AdvancedAnalysisRequest for analysis purposes
     */
    public AdvancedAnalysisRequest toAdvancedAnalysisRequest() {
        return new AdvancedAnalysisRequest(image, useMockData, userServingSize, temperature);
    }
}