package com.aliyaman.icqa_shein.analytics.workefficiency.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "work_efficiency_metrics")
public class WorkEfficiencyMetrics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "record_date", nullable = false, unique = true)
    private LocalDate recordDate;

    @Builder.Default
    @Column(name = "abnormal_second_sorting_sku", nullable = false)
    private Integer abnormalSecondSortingSku = 0;

    @Builder.Default
    @Column(name = "inventory_sku", nullable = false)
    private Integer inventorySku = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = Instant.now();
        if (abnormalSecondSortingSku == null) abnormalSecondSortingSku = 0;
        if (inventorySku == null) inventorySku = 0;
    }
}