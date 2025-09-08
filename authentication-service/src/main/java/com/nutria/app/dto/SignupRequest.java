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
    private int age;
    private String gender;
    private double height;
    private double weight;
    private double weightGoal;
    private String activityLevel;
    private String caloricAdjustment;

/*
{
  "name": "John",
  "lastName": "Doe",
  "email": "john.doe@example.com",
  "password": "StrongPassword123",
  "age": 30,
  "gender": "MALE",
  "height": 175,
  "weight": 70,
  "weightGoal": 65,
  "activityLevel": "MODERATE",
  "caloricAdjustment": "LOSE_050"
}
 */
}
