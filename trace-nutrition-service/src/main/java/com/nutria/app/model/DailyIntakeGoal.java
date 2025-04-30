package com.nutria.app.model;


import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.Date;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "daily_intake_goal")
public class DailyIntakeGoal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private long userId;
    private double calories;
    private double proteins;
    private double carbs;
    private double fats;
    private double weightGoal;
    private Date recordAt;

    @CreationTimestamp
    private LocalDateTime createdAt;

}
