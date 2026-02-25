package com.aliyaman.icqa_shein.audits.inventorycheck.excel;


import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
public class InventoryCheckExcelReader {

    // Expected headers
    public static final String H_LOCATION = "Location";
    public static final String H_SKU = "sku";
    public static final String H_OPERATOR = "Counting operator";
    public static final String H_COLLECTION_TIME = "Collection time";
    public static final String H_DIFF_FLAG = "Different or not"; // optional
    public static final String H_DIFFERENCES = "Differences";     // optional

    public ParsedSheet read(MultipartFile file) {
        try (InputStream is = file.getInputStream();
             Workbook wb = new XSSFWorkbook(is)) {

            Sheet sheet = wb.getSheetAt(0);
            if (sheet == null || sheet.getPhysicalNumberOfRows() < 2) {
                return new ParsedSheet(Collections.emptyList());
            }

            Map<String, Integer> col = headerIndex(sheet.getRow(0));

            int cLoc = required(col, H_LOCATION);
            int cSku = required(col, H_SKU);
            int cOp = required(col, H_OPERATOR);
            int cTime = required(col, H_COLLECTION_TIME);

            Integer cDiffFlag = col.get(H_DIFF_FLAG);
            Integer cDifferences = col.get(H_DIFFERENCES);

            List<RowModel> rows = new ArrayList<>();

            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                String location = str(row.getCell(cLoc));
                String sku = str(row.getCell(cSku));
                String operator = str(row.getCell(cOp));
                LocalDateTime t = parseDateTime(row.getCell(cTime));

                // skip empty row
                if (isBlank(location) && isBlank(sku) && isBlank(operator) && t == null) continue;

                String diffFlag = (cDiffFlag == null) ? "" : str(row.getCell(cDiffFlag));
                long diffUnits = (cDifferences == null) ? 0 : longNum(row.getCell(cDifferences));

                rows.add(new RowModel(location, sku, operator, t, diffFlag, diffUnits));
            }

            return new ParsedSheet(rows);

        } catch (Exception e) {
            throw new RuntimeException("InventoryCheckExcelReader failed: " + e.getMessage(), e);
        }
    }

    // ---------------- Models returned to service (NO record) ----------------

    public static class ParsedSheet {
        private final List<RowModel> rows;

        public ParsedSheet(List<RowModel> rows) {
            this.rows = rows;
        }

        public List<RowModel> getRows() {
            return rows;
        }
    }

    public static class RowModel {
        private final String location;
        private final String sku;
        private final String operator;
        private final LocalDateTime collectionTime;
        private final String differentOrNot;
        private final long differences;

        public RowModel(String location,
                        String sku,
                        String operator,
                        LocalDateTime collectionTime,
                        String differentOrNot,
                        long differences) {
            this.location = location;
            this.sku = sku;
            this.operator = operator;
            this.collectionTime = collectionTime;
            this.differentOrNot = differentOrNot;
            this.differences = differences;
        }

        public String getLocation() { return location; }
        public String getSku() { return sku; }
        public String getOperator() { return operator; }
        public LocalDateTime getCollectionTime() { return collectionTime; }
        public String getDifferentOrNot() { return differentOrNot; }
        public long getDifferences() { return differences; }
    }

    // ---------------- Helpers ----------------

    private Map<String, Integer> headerIndex(Row headerRow) {
        Map<String, Integer> map = new HashMap<>();
        if (headerRow == null) return map;

        for (Cell cell : headerRow) {
            String h = str(cell);
            if (!isBlank(h)) map.put(h.trim(), cell.getColumnIndex());
        }
        return map;
    }

    private int required(Map<String, Integer> map, String header) {
        Integer idx = map.get(header);
        if (idx == null) throw new IllegalArgumentException("Missing header column: " + header);
        return idx;
    }

    private String str(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    var dt = cell.getDateCellValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
                    yield dt.toString();
                }
                double v = cell.getNumericCellValue();
                if (Math.floor(v) == v) yield String.valueOf((long) v);
                yield String.valueOf(v);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> {
                try { yield cell.getStringCellValue(); }
                catch (Exception e) {
                    try { yield String.valueOf(cell.getNumericCellValue()); }
                    catch (Exception ee) { yield ""; }
                }
            }
            default -> "";
        };
    }

    private long longNum(Cell cell) {
        if (cell == null) return 0;
        try {
            if (cell.getCellType() == CellType.NUMERIC) return Math.round(cell.getNumericCellValue());
            String s = str(cell);
            if (isBlank(s)) return 0;
            return Math.round(Double.parseDouble(s.replace(",", ".").trim()));
        } catch (Exception e) {
            return 0;
        }
    }

    private LocalDateTime parseDateTime(Cell cell) {
        if (cell == null) return null;

        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getDateCellValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        }

        String s = str(cell);
        if (isBlank(s)) return null;

        List<DateTimeFormatter> fmts = List.of(
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
                DateTimeFormatter.ISO_LOCAL_DATE_TIME // fallback
        );

        for (var f : fmts) {
            try { return LocalDateTime.parse(s.trim(), f); }
            catch (Exception ignored) {}
        }
        return null;
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}