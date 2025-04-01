package com.nutria.app.dto;

import lombok.Data;

@Data
public class MacrosDataRequest {

    private String description;
    private double calories;
    private double proteins;
    private double carbs;
    private double fats;
    private double servingSize;

}
