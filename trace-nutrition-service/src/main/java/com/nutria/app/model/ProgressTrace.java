package com.nutria.app.model;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "progress_trace")
public class ProgressTrace {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private LocalDateTime dateInit;
    private LocalDateTime dateEnt;
    private double calories_consumed;
    private double proteins_consumed;
    private double carbs_consumed;
    private double fats_consumed;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "weight_trace_id", referencedColumnName = "id")
    private WeightTrace weightTrace;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

}
