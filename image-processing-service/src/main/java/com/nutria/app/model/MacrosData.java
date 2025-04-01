package com.nutria.app.model;


import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Data
@Document(collection = "macros_data")
public class MacrosData {

    @Id
    private String id;
    private Long userId;
    private String description;
    private double calories;
    private double proteins;
    private double carbs;
    private double fats;
    private double servingSize;
    private String status;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(updatable = false)
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
