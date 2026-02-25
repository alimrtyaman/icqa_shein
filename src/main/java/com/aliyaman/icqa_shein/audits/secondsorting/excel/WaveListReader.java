package com.aliyaman.icqa_shein.audits.secondsorting.excel;

import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Component
public class WaveListReader {

    public Map<String, LocalDateTime> readWaveEndTimes(MultipartFile file) {
        try (InputStream in = file.getInputStream();
             Workbook wb = WorkbookFactory.create(in)) {

            Sheet sheet = wb.getSheetAt(0);
            if (sheet == null) return Map.of();

            Row header = sheet.getRow(sheet.getFirstRowNum());
            Map<String, Integer> idx = headerIndex(header);

            Integer waveIdx = findIdx(idx, "wave number", "wave no", "wave", "waveno", "wave_no");
            Integer endIdx  = findIdx(idx, "end time", "endtime", "end");

            if (waveIdx == null || endIdx == null) {
                throw new RuntimeException("Cannot find required columns in WaveExecution.xlsx. Found headers: " + idx.keySet());
            }

            Map<String, LocalDateTime> map = new HashMap<>();

            for (int r = header.getRowNum() + 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                String waveNo = ExcelCell.normalizeWaveNo(ExcelCell.asString(row.getCell(waveIdx)));
                if (waveNo == null) continue;

                LocalDateTime endTime = ExcelCell.asDateTime(row.getCell(endIdx));
                if (endTime != null) map.putIfAbsent(waveNo, endTime);
            }

            return map;

        } catch (Exception e) {
            throw new RuntimeException("Failed to read WaveExecution.xlsx: " + e.getMessage(), e);
        }
    }

    private Map<String, Integer> headerIndex(Row header) {
        Map<String, Integer> map = new HashMap<>();
        if (header == null) return map;

        for (Cell c : header) {
            String key = ExcelCell.asString(c);
            if (key != null) map.put(key.toLowerCase(), c.getColumnIndex());
        }
        return map;
    }

    private Integer findIdx(Map<String, Integer> idx, String... names) {
        for (String n : names) {
            Integer i = idx.get(n.toLowerCase());
            if (i != null) return i;
        }
        return null;
    }
}
