package com.aliyaman.icqa_shein.audits.abnormalsorting.excel;

import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
public class AbnormalSortingExcelReader {

    public List<RowModel> read(MultipartFile file) {
        try (InputStream in = file.getInputStream();
             Workbook wb = WorkbookFactory.create(in)) {

            Sheet sheet = wb.getSheetAt(0);
            if (sheet == null) return List.of();

            int firstRow = sheet.getFirstRowNum();
            Row header = sheet.getRow(firstRow);
            if (header == null) return List.of();

            Map<String, Integer> idx = headerIndex(header);

            Integer operatorIdx = findIdx(idx, "operator");
            Integer timeIdx = findIdx(idx, "operation time", "operationtime", "time", "timestamp", "date time", "datetime");
            Integer qtyIdx = findIdx(idx, "2nd sorting quantity", "2nd quantity", "second sorting quantity", "qty", "quantity");
            Integer skuIdx = findIdx(idx, "skucode", "sku code", "sku");

            if (operatorIdx == null || timeIdx == null || qtyIdx == null || skuIdx == null) {
                throw new RuntimeException("Missing required columns. Found headers: " + idx.keySet());
            }

            List<RowModel> rows = new ArrayList<>();

            for (int r = firstRow + 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                String operator = asString(row.getCell(operatorIdx));
                if (operator == null || operator.isBlank()) continue;

                LocalDateTime ts = asDateTime(row.getCell(timeIdx));
                if (ts == null) continue;

                Long qty = asLong(row.getCell(qtyIdx));
                if (qty == null) qty = 0L;

                String sku = asString(row.getCell(skuIdx));
                if (sku == null) sku = "";

                rows.add(new RowModel(operator.trim(), sku.trim(), qty, ts));
            }

            return rows;

        } catch (Exception e) {
            throw new RuntimeException("Failed to read AbnormalSorting excel: " + e.getMessage(), e);
        }
    }

    private Map<String, Integer> headerIndex(Row header) {
        Map<String, Integer> map = new HashMap<>();
        for (Cell c : header) {
            String key = asString(c);
            if (key != null) map.put(key.trim().toLowerCase(Locale.ROOT), c.getColumnIndex());
        }
        return map;
    }

    private Integer findIdx(Map<String, Integer> idx, String... names) {
        for (String n : names) {
            Integer i = idx.get(n.toLowerCase(Locale.ROOT));
            if (i != null) return i;
        }
        return null;
    }

    private String asString(Cell cell) {
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING -> clean(cell.getStringCellValue());
            case NUMERIC -> {
                double d = cell.getNumericCellValue();
                if (Math.floor(d) == d) yield String.valueOf((long) d);
                yield String.valueOf(d);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> {
                try { yield clean(cell.getStringCellValue()); }
                catch (Exception e) { yield null; }
            }
            default -> null;
        };
    }

    private Long asLong(Cell cell) {
        if (cell == null) return null;
        try {
            return switch (cell.getCellType()) {
                case NUMERIC -> (long) cell.getNumericCellValue();
                case STRING -> {
                    String s = asString(cell);
                    yield (s == null || s.isBlank()) ? null : Long.parseLong(s.trim());
                }
                default -> null;
            };
        } catch (Exception e) {
            return null;
        }
    }

    private LocalDateTime asDateTime(Cell cell) {
        if (cell == null) return null;

        try {
            if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                return LocalDateTime.ofInstant(cell.getDateCellValue().toInstant(), ZoneId.systemDefault());
            }

            String s = asString(cell);
            if (s == null || s.isBlank()) return null;

            s = s.trim();
            DateTimeFormatter[] fmts = new DateTimeFormatter[]{
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
                    DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"),
                    DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm"),
                    DateTimeFormatter.ISO_LOCAL_DATE_TIME
            };

            for (DateTimeFormatter f : fmts) {
                try { return LocalDateTime.parse(s, f); } catch (Exception ignored) {}
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private String clean(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    public static class RowModel {
        private final String operator;
        private final String skuCode;
        private final long qty;
        private final LocalDateTime time;

        public RowModel(String operator, String skuCode, long qty, LocalDateTime time) {
            this.operator = operator;
            this.skuCode = skuCode;
            this.qty = qty;
            this.time = time;
        }

        public String getOperator() { return operator; }
        public String getSkuCode() { return skuCode; }
        public long getQty() { return qty; }
        public LocalDateTime getTime() { return time; }
    }
}