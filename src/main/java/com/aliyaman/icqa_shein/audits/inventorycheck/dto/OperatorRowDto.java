package com.aliyaman.icqa_shein.audits.inventorycheck.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperatorRowDto {

    private String operator;
    private long locations;
    private long sku;
    private String time;
    private long bph;
    private long discrepancies;
    private double accuracyPct;
}