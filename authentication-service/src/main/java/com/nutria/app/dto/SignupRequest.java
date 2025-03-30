package com.nutria.app.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignupRequest {
    private String email;
    private String password;
    private String name;
    private String lastName;
    private int age;
    private String gender;
    private double height;
    private double weight;
    private double weightGoal;
    private String activityLevel;
    private String caloricAdjustment;
}
