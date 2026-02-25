package com.aliyaman.icqa_shein.audits.secondsorting.excel;


import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Component
public class ExceptionListReader {

    public Map<String, WaveAgg> readSecondSortingAgg(MultipartFile exceptionFile) {
        Map<String, WaveAgg> agg = new HashMap<>();

        try (InputStream in = exceptionFile.getInputStream();
             Workbook wb = WorkbookFactory.create(in)) {

            Sheet sheet = wb.getSheetAt(0);
            if (sheet == null) return agg;

            Row header = sheet.getRow(sheet.getFirstRowNum());
            Map<String, Integer> idx = headerIndex(header);

            Integer waveIdx = findIdx(idx, "wave number", "wave no", "wave", "waveno", "wave_no");
            Integer shelvingIdx = findIdx(idx,
                    "abnormal shelving time",
                    "shelving time", "shelvingtime", "shelving",
                    "scan time", "scantime",
                    "timestamp", "time", "date time", "datetime", "date"
            );

            if (waveIdx == null || shelvingIdx == null) {
                throw new RuntimeException("Cannot find required columns in Package-exception.xlsx. Found headers: " + idx.keySet());
            }

            for (int r = header.getRowNum() + 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                String waveNo = ExcelCell.normalizeWaveNo(ExcelCell.asString(row.getCell(waveIdx)));
                if (waveNo == null) continue;

                LocalDateTime shelvingTime = ExcelCell.asDateTime(row.getCell(shelvingIdx));
                if (shelvingTime == null) continue;

                agg.merge(
                        waveNo,
                        new WaveAgg(1L, shelvingTime),
                        (oldAgg, n) -> new WaveAgg(
                                oldAgg.count() + 1L,
                                oldAgg.maxShelvingTime() == null
                                        ? shelvingTime
                                        : (shelvingTime.isAfter(oldAgg.maxShelvingTime())
                                        ? shelvingTime
                                        : oldAgg.maxShelvingTime())
                        )
                );
            }

            return agg;

        } catch (Exception e) {
            throw new RuntimeException("Failed to read Package-exception.xlsx: " + e.getMessage(), e);
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
