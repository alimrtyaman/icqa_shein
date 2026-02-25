package com.aliyaman.icqa_shein.analytics.workefficiency.repository;


import com.aliyaman.icqa_shein.analytics.workefficiency.entity.WorkEfficiencyMetrics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface WorkEfficiencyMetricsRepository extends JpaRepository<WorkEfficiencyMetrics, Long> {

    Optional<WorkEfficiencyMetrics> findByRecordDate(LocalDate recordDate);

    List<WorkEfficiencyMetrics> findByRecordDateBetweenOrderByRecordDateAsc(LocalDate start, LocalDate end);
}