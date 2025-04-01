package com.nutria.app.repository;

import com.nutria.app.model.MacrosData;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface MacrosDataRepository extends MongoRepository<MacrosData, String> {
    List<MacrosData> findMacrosDataByUserId(Long userId);
}
