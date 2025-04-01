package com.nutria.app.repository;

import com.nutria.app.model.FoodImage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FoodImageRepository extends JpaRepository<FoodImage, Long> {
}
