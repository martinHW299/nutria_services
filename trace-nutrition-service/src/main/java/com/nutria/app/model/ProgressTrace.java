package com.nutria.app.model;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "progress_trace")
public class ProgressTrace {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private long userId;
    private Date dateInit;
    private Date dateEnt;
    private double caloriesConsumed;
    private double proteinsConsumed;
    private double carbsConsumed;
    private double fatsConsumed;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "weight_trace_id", referencedColumnName = "id")
    private WeightTrace weightTrace;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

}
