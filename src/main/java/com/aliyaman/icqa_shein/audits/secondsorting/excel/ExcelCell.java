package com.aliyaman.icqa_shein.audits.secondsorting.excel;


import org.apache.poi.ss.usermodel.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public final class ExcelCell {
    private ExcelCell() {}

    public static String asString(Cell cell) {
        if (cell == null) return null;

        return switch (cell.getCellType()) {
            case STRING -> clean(cell.getStringCellValue());
            case NUMERIC -> clean(String.valueOf((long) cell.getNumericCellValue()));
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> {
                try { yield clean(cell.getStringCellValue()); }
                catch (Exception e) { yield null; }
            }
            default -> null;
        };
    }

    public static LocalDateTime asDateTime(Cell cell) {
        if (cell == null) return null;

        try {
            if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                Date d = cell.getDateCellValue();
                return LocalDateTime.ofInstant(d.toInstant(), ZoneId.systemDefault());
            }

            String s = asString(cell);
            if (s == null || s.isBlank()) return null;

            s = s.trim();
            DateTimeFormatter[] fmts = new DateTimeFormatter[] {
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
                    DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"),
                    DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm")
            };

            for (var f : fmts) {
                try { return LocalDateTime.parse(s, f); } catch (Exception ignored) {}
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    public static String normalizeWaveNo(String s) {
        if (s == null) return null;
        s = s.trim();
        if (s.isEmpty()) return null;
        return s.toUpperCase();
    }

    private static String clean(String s) {
        if (s == null) return null;
        s = s.trim();
        return s.isEmpty() ? null : s;
    }
}
