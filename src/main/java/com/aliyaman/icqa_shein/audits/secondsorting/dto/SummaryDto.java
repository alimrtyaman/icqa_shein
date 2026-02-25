package com.aliyaman.icqa_shein.audits.secondsorting.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SummaryDto {
    private long totalExceptions;
    private long matchedWaves;
    private long missingEndTimeWaves;
}
