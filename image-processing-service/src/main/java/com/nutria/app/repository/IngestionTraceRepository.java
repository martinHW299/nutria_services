package com.nutria.app.repository;

import com.nutria.app.model.IngestionTrace;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IngestionTraceRepository extends JpaRepository<IngestionTrace, Long> {

}
