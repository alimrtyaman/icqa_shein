package com.aliyaman.icqa_shein.analytics.workefficiency.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkEfficiencyMetricsPointDto {

    private LocalDate date;

    private int abnormalSecondSortingSku;

    private int inventorySku;
}