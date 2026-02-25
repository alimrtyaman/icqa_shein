package com.aliyaman.icqa_shein.audits.abnormalsorting.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AbnormalSortingResponse {
    private SessionStatsDto session;
    private TopPerformersDto topPerformers;
    private List<OperatorRowDto> results;
}
