package com.nutria.app.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
public class SignupRequest {
    private String email;
    private String password;
    private String name;
    private String lastName;
    private Integer age;
    private String gender;
    private Double height;
    private Double weight;
    private Double weightGoal;
    private String activityLevel;
    private String caloricAdjustment;
}
