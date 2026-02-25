package com.aliyaman.icqa_shein.audits.abnormalsorting.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SessionStatsDto {
    private long volume;
    private long avgUph;
}