package com.aliyaman.icqa_shein.audits.inventorycheck.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryCheckResponse {

    private SessionStatsDto sessionStats;
    private TopPerformersDto topPerformers;
    private List<OperatorRowDto> operators;
}
