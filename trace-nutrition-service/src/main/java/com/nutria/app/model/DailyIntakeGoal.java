package com.nutria.app.model;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "daily_intake_goal")
public class DailyIntakeGoal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private double calories;
    private double proteins;
    private double carbs;
    private double fats;
    private double weightGoal;

    @CreationTimestamp
    private LocalDateTime createdAt;

}
