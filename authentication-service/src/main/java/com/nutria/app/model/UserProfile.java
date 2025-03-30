package com.nutria.app.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_profile")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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

    private double bmr;

    private double bmi;

    private double tdee;

    private double caloricAdjustment;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(updatable = false)
    private LocalDateTime updatedAt;

    @Getter
    public enum Gender {
        MALE("M", 5),
        FEMALE("F", 161);

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


    @Getter
    public enum CaloricAdjustment {

        MAINTAIN(0),
        LOSS_LIGHT(-0.1), //10%
        LOSS_MODERATE(-0.2), //20%
        GAIN_LIGHT(0.1), //10%
        GAIN_MODERATE(0.15), //15%
        GAIN_AGGRESSIVE(0.2), //20%
        GAIN_VERY_AGGRESSIVE(0.25); //25%

        private final double value;

        CaloricAdjustment(double value) {
            this.value = value;
        }
    }


}
