package com.aliyaman.icqa_shein.audits.abnormalsorting.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OperatorRowDto {
    private String operator;
    private long sku;
    private String time; // "H:mm"
    private long uph;
}
