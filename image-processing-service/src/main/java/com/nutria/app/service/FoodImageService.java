package com.nutria.app.service;


import com.nutria.app.model.FoodImage;
import com.nutria.app.repository.FoodImageRepository;
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

    @Value("${image.path.upload}")
    private String FOLDER_PATH;

    private final FoodImageRepository foodImageRepository;

    public FoodImage saveFoodImage(Long userId, String imageBase64) throws IOException {

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
    }

}
