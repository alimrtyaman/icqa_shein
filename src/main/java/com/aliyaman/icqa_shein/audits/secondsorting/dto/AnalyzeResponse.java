package com.aliyaman.icqa_shein.audits.secondsorting.dto;

import com.aliyaman.icqa_shein.audits.secondsorting.dto.SummaryDto;
import com.aliyaman.icqa_shein.audits.secondsorting.dto.WaveResultDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AnalyzeResponse {
    private SummaryDto summary;
    private List<WaveResultDto> waves;
}
