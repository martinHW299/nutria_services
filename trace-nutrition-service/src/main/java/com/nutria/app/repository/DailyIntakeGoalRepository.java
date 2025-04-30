package com.nutria.app.repository;

import com.nutria.app.model.DailyIntakeGoal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import javax.swing.text.html.Option;
import java.util.Date;
import java.util.Optional;


public interface DailyIntakeGoalRepository extends JpaRepository<DailyIntakeGoal, Long> {

    @Query("""
    from DailyIntakeGoal a
    where a.userId = :id and cast(a.recordAt as date) <= :targetDate
    order by cast(a.recordAt as date) desc limit 1
    """)
    Optional<DailyIntakeGoal> getDailyIntakeGoalByUserId(@Param("id") Long userId, @Param("targetDate") Date targetDate);
}
