package com.nutria.app.repository;

import com.nutria.app.model.IngestionTrace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface IngestionTraceRepository extends JpaRepository<IngestionTrace, Long> {

    @Query("from IngestionTrace a where a.userId = :id and a.status = 'AC' and cast(a.createdAt as date) between :initDate and :endDate order by cast(a.createdAt as date)")
    List<IngestionTrace>getDailyIntakeByDateRange(@Param("id") Long id, @Param("initDate")Date initDate, @Param("endDate")Date endDate);

    Optional<IngestionTrace> getIngestionTraceById(Long id);
}
