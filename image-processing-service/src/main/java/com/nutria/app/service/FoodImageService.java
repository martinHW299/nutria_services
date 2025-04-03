package com.nutria.app.service;


import com.nutria.app.model.FoodImage;
import com.nutria.app.repository.FoodImageRepository;
import com.nutria.common.exceptions.ValidationException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FoodImageService {

    //@Value("${image.path.upload}")
    @Value("${FOOD_IMAGES_DIR:/app/food-images}")
    private String FOLDER_PATH;

    private final FoodImageRepository foodImageRepository;

    @PostConstruct
    public void init() {
        try {
            Path path = Paths.get(FOLDER_PATH);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory", e);
        }
    }
    public FoodImage saveFoodImage(Long userId, String imageBase64) {
        try{
            Path uploadPath = Paths.get(FOLDER_PATH);
            byte[] imageBytes = Base64.getDecoder().decode(imageBase64);
            String generatedId = UUID.randomUUID().toString();

            String filename = LocalDateTime.now() + "_urs_" + userId + "_file_" + generatedId + ".png";
            Path filePath = uploadPath.resolve(filename);

            Files.write(filePath, imageBytes);

            FoodImage foodImage = FoodImage.builder()
                    .id(generatedId)
                    .userId(userId)
                    .fileName(filename)
                    .filePath(filePath.toString())
                    .build();

            return foodImageRepository.save(foodImage);
        } catch (IOException e) {
            throw new ValidationException("Failed to save image");
        }
    }

}
