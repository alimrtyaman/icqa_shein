package com.aliyaman.icqa_shein.audits.abnormalsorting.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TopPerformersDto {
    private List<OperatorRowDto> byUph;
    private List<OperatorRowDto> bySku;
}