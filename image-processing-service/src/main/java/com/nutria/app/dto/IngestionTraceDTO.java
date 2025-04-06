package com.nutria.app.dto;

import com.nutria.app.model.FoodImage;
import com.nutria.app.model.MacrosData;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class IngestionTraceDTO {

    private Long id;
    private Long userId;
    private MacrosData macrosData;  // fetched from MongoDB
    private FoodImage foodImage;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
