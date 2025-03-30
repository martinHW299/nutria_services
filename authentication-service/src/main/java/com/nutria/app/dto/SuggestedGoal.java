package com.nutria.app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SuggestedGoal {

    private double minWeight;
    private double maxWeight;
    private String suggestedGoal;

}
