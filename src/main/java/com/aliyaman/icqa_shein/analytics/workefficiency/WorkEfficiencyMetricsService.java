package com.aliyaman.icqa_shein.analytics.workefficiency;


import com.aliyaman.icqa_shein.analytics.workefficiency.dto.WorkEfficiencyMetricsPointDto;
import com.aliyaman.icqa_shein.analytics.workefficiency.entity.WorkEfficiencyMetrics;
import com.aliyaman.icqa_shein.analytics.workefficiency.repository.WorkEfficiencyMetricsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WorkEfficiencyMetricsService {

    private final WorkEfficiencyMetricsRepository repo;

    private static final ZoneId ZONE = ZoneId.of("Europe/Berlin");

    public LocalDate today() {
        return LocalDate.now(ZONE);
    }

    /**
     * Updates only abnormalSecondSortingSku for given date.
     * Does NOT overwrite inventorySku.
     */
    @Transactional
    public void updateAbnormalSku(LocalDate date, int abnormalSku) {
        upsertSafely(date, (m) -> m.setAbnormalSecondSortingSku(Math.max(0, abnormalSku)));
    }

    /**
     * Updates only inventorySku for given date.
     * Does NOT overwrite abnormalSecondSortingSku.
     */
    @Transactional
    public void updateInventorySku(LocalDate date, int inventorySku) {
        upsertSafely(date, (m) -> m.setInventorySku(Math.max(0, inventorySku)));
    }

    /**
     * Read range for chart (daily points).
     */
    @Transactional(readOnly = true)
    public List<WorkEfficiencyMetricsPointDto> getRange(LocalDate start, LocalDate end) {
        return repo.findByRecordDateBetweenOrderByRecordDateAsc(start, end)
                .stream()
                .map(this::toPointDto)
                .toList();
    }

    // ---------------- Internals ----------------

    private WorkEfficiencyMetricsPointDto toPointDto(WorkEfficiencyMetrics m) {
        return WorkEfficiencyMetricsPointDto.builder()
                .date(m.getRecordDate())
                .abnormalSecondSortingSku(nvl(m.getAbnormalSecondSortingSku()))
                .inventorySku(nvl(m.getInventorySku()))
                .build();
    }

    private int nvl(Integer v) {
        return v == null ? 0 : v;
    }

    @FunctionalInterface
    private interface Mutator {
        void apply(WorkEfficiencyMetrics metrics);
    }

    /**
     * Upsert with protection against unique(recordDate) race:
     * - Try create if missing
     * - If unique constraint hits (two threads inserted same date), load existing and retry once.
     */
    private void upsertSafely(LocalDate date, Mutator mutator) {
        try {
            WorkEfficiencyMetrics m = repo.findByRecordDate(date).orElseGet(() ->
                    WorkEfficiencyMetrics.builder()
                            .recordDate(date)
                            .abnormalSecondSortingSku(0)
                            .inventorySku(0)
                            .build()
            );

            // Apply only the metric we want to update
            mutator.apply(m);

            // Keep recordDate always set
            m.setRecordDate(date);

            repo.save(m);

        } catch (DataIntegrityViolationException ex) {
            // Rare case: concurrent insert for same date because of unique constraint.
            // Load existing and retry once.
            WorkEfficiencyMetrics existing = repo.findByRecordDate(date)
                    .orElseThrow(() -> ex);

            mutator.apply(existing);
            existing.setRecordDate(date);

            repo.save(existing);
        }
    }
}