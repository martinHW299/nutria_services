package com.nutria.app.service;


import com.nutria.app.dto.IngestionTraceDTO;
import com.nutria.app.dto.MacrosDataRequest;
import com.nutria.app.model.FoodImage;
import com.nutria.app.model.IngestionTrace;
import com.nutria.app.model.MacrosData;
import com.nutria.app.repository.IngestionTraceRepository;
import com.nutria.common.exceptions.ResourceNotFoundException;
import com.nutria.common.exceptions.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class IngestionTraceService {

    private final JwtService jwtService;
    private final MacrosDataService macrosDataService;
    private final FoodImageService foodImageService;
    private final IngestionTraceRepository ingestionTraceRepository;

    public IngestionTrace save(String token, Date date, String image, MacrosData macrosData) {

        Long userId = jwtService.extractId(token);
        FoodImage foodImage = foodImageService.saveFoodImage(userId, image);

        macrosDataService.saveMacros(token, macrosData);

        IngestionTrace ingestionTrace = IngestionTrace.builder()
                .userId(userId)
                .macrosDataId(macrosData.getId())
                .foodImage(foodImage)
                .status(IngestionTrace.DataStatus.ACTIVE.getCode())
                .recordAt(date)
                .build();

        return ingestionTraceRepository.save(ingestionTrace);

    }

    public List<IngestionTraceDTO> getIngestionTraceByPeriod(String token, Date dateInit, Date dateEnd) {
        try {
            Long userId = jwtService.extractId(token);
            List<IngestionTrace> ingestionTraceList = ingestionTraceRepository.getDailyIntakeByDateRange(userId, dateInit, dateEnd);
            return ingestionTraceList.stream()
                    .map(ingestionTrace -> {
                        MacrosData macrosData;
                        try {
                            macrosData = macrosDataService.getById(ingestionTrace.getMacrosDataId());
                        } catch (ResourceNotFoundException e) {
                            throw new ResourceNotFoundException("Macro nutrients value of the food was not found");
                        }
                        return new IngestionTraceDTO(
                                ingestionTrace.getId(),
                                ingestionTrace.getUserId(),
                                macrosData,
                                ingestionTrace.getFoodImage(),
                                ingestionTrace.getStatus(),
                                ingestionTrace.getRecordAt(),
                                ingestionTrace.getCreatedAt(),
                                ingestionTrace.getUpdatedAt()
                        );
                    }).collect(Collectors.toList());
        } catch (Exception e) {
            throw new ValidationException("Periodic request of ingestion tracing failed");
        }
    }

    public IngestionTrace delete(Long id) {
        IngestionTrace ingestionTrace = ingestionTraceRepository.getIngestionTraceById(id).orElseThrow(() -> new ValidationException("Intake data not found"));
        ingestionTrace.setStatus(IngestionTrace.DataStatus.INACTIVE.getCode());
        return ingestionTraceRepository.save(ingestionTrace);
    }
}
