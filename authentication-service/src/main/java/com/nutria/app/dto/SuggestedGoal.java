package com.nutria.app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SuggestedGoal {

    private Double minWeight;
    private Double maxWeight;
    private String suggestedGoal;

}
