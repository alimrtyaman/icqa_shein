package com.aliyaman.icqa_shein.audits.inventorycheck.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionStatsDto {

    private long totalRows;
    private long totalLocations;
    private long totalSku;
    private long discrepancies;
    private double accuracyPct;
    private long varianceUnits;
    private long avgBph;
    private long maxBph;
}
