package com.nutria.app.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "user_profiles")
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private UserCredential userCredential;

    private String userName;
    private String userLastname;
    private int age;
    private String gender;
    private double height;
    private double weight;
    private double weightGoal;
    private double activityLevel;
    private String caloricAdjustment;
    private double bmr;
    private double bmi;
    private double tdee;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(updatable = false)
    private LocalDateTime updatedAt;

    @Getter
    public enum Gender {
        MALE("M", 5),
        FEMALE("F", -161);

        private final String code;
        private final int value;

        Gender(String code, int value) {
            this.code = code;
            this.value = value;
        }
    }

    @Getter
    public enum ActivityLevel {
        SEDENTARY(1.2),
        LIGHTLY(1.375),
        MODERATE(1.55),
        VERY(1.725),
        SUPER(1.9);

        private final double value;

        ActivityLevel(double value) {
            this.value = value;
        }
    }

    @Getter
    public enum BmiBalance {
        LOSS("LOSS", 25.0, Float.MAX_VALUE),
        MAINTAIN("MAINTAIN", 18.5, 24.9),
        GAIN("GAIN", 0.0, 18.5);

        private final String description;
        private final double minBmi;
        private final double maxBmi;

        BmiBalance(String description, double minBmi, double maxBmi) {
            this.description = description;
            this.minBmi = minBmi;
            this.maxBmi = maxBmi;
        }

        public boolean isWithinRange(double bmi) {
            return bmi >= minBmi && bmi < maxBmi;
        }
    }


    public enum CaloricAdjustment {
        LOSE_025(-0.25, "Lose 0.25 kg/week"),
        LOSE_050(-0.50, "Lose 0.5 kg/week"),
        LOSE_100(-1.00, "Lose 1 kg/week"),
        MAINTAIN(0.0, "Maintain weight"),
        GAIN_025(0.25, "Gain 0.25 kg/week"),
        GAIN_050(0.50, "Gain 0.5 kg/week");

        private final double kgPerWeek;
        private final String description;

        CaloricAdjustment(double kgPerWeek, String description) {
            this.kgPerWeek = kgPerWeek;
            this.description = description;
        }

        /** Value in kg/week (positive = gain, negative = loss) */
        public double getValue() {
            return kgPerWeek;
        }

        public String getDescription() {
            return description;
        }
    }



}
