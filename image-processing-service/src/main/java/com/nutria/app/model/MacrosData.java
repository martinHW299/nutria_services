package com.nutria.app.model;


import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Data
@Document(collection = "macros_data")
public class MacrosData {

    @Id
    private String id;
    private long userId;
    private String description;
    private double calories;
    private double proteins;
    private double carbs;
    private double fats;
    private double servingSize;
    private String status;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Getter
    public enum MacrosStatus {
        ACTIVE("AC"),
        INACTIVE("IN");

        private final String code;

        MacrosStatus(String code) {
            this.code = code;
        }
    }
}
