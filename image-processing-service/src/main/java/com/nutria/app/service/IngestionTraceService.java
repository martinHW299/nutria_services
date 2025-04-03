package com.nutria.app.service;


import com.nutria.app.model.FoodImage;
import com.nutria.app.model.IngestionTrace;
import com.nutria.app.model.MacrosData;
import com.nutria.app.repository.IngestionTraceRepository;
import com.nutria.common.exceptions.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.util.Date;
import java.util.List;


@Service
@RequiredArgsConstructor
public class IngestionTraceService {

    private final JwtService jwtService;
    private final MacrosDataService macrosDataService;
    private final FoodImageService foodImageService;
    private final IngestionTraceRepository ingestionTraceRepository;

    public IngestionTrace save(String token, String imagesBase64) {

        Long userId = jwtService.extractId(token);
        MacrosData macrosData = macrosDataService.saveMacros(userId, imagesBase64);
        FoodImage foodImage = foodImageService.saveFoodImage(userId, imagesBase64);

        IngestionTrace ingestionTrace = IngestionTrace.builder()
                .userId(userId)
                .macrosDataId(macrosData.getId())
                .foodImage(foodImage)
                .status(IngestionTrace.DataStatus.ACTIVE.getCode())
                .build();

        return ingestionTraceRepository.save(ingestionTrace);
    }

    public List<IngestionTrace> getIngestionTraceByPeriod(String token, Date dateInit, Date dateEnd) {
        try {
            if (dateInit.after(dateEnd)) {
                throw new ValidationException("Initial date can not be higher that final date");
            } else {
                Long userId = jwtService.extractId(token);
                return ingestionTraceRepository.getDailyIntakeByDateRange(userId, dateInit, dateEnd);
            }
        } catch (Exception e) {
            throw new ValidationException("Periodic request of ingestion tracing failed");
        }
    }
}
