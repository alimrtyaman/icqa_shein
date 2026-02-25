package com.aliyaman.icqa_shein.audits.abnormalsorting;

import com.aliyaman.icqa_shein.analytics.workefficiency.WorkEfficiencyMetricsService;
import com.aliyaman.icqa_shein.audits.abnormalsorting.dto.*;
import com.aliyaman.icqa_shein.audits.abnormalsorting.excel.AbnormalSortingExcelReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;


@Service
public class AbnormalSortingService {

    private final AbnormalSortingExcelReader reader;
    private final WorkEfficiencyMetricsService metricsService;

    // Constructor (Eğer Spring kullanıyorsan @RequiredArgsConstructor kullanabilirsin)
    public AbnormalSortingService(AbnormalSortingExcelReader reader, WorkEfficiencyMetricsService metricsService) {
        this.reader = reader;
        this.metricsService = metricsService;
    }

    public AbnormalSortingResponse analyze(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("File is empty");

        List<AbnormalSortingExcelReader.RowModel> rows = reader.read(file);

        if (rows == null || rows.isEmpty()) {
            return new AbnormalSortingResponse(
                    new SessionStatsDto(0, 0),
                    new TopPerformersDto(List.of(), List.of()),
                    List.of()
            );
        }

        // -------------------------
        // SESSION STATS
        // -------------------------
        LocalDateTime sessionStart = rows.stream()
                .map(AbnormalSortingExcelReader.RowModel::getTime)
                .filter(Objects::nonNull)
                .min(LocalDateTime::compareTo)
                .orElse(null);

        LocalDateTime sessionEnd = rows.stream()
                .map(AbnormalSortingExcelReader.RowModel::getTime)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        double sessionMinutes = minutesBetweenPrecise(sessionStart, sessionEnd);

        long sessionVolume = rows.stream()
                .mapToLong(AbnormalSortingExcelReader.RowModel::getQty)
                .sum();

        long sessionAvgUph = calcUph(sessionVolume, sessionMinutes);

        // -------------------------
        // GROUP BY OPERATOR
        // -------------------------
        Map<String, List<AbnormalSortingExcelReader.RowModel>> byOp =
                rows.stream().collect(Collectors.groupingBy(r -> safeStr(r.getOperator())));

        List<OperatorRowDto> operatorRows = new ArrayList<>();

        for (var e : byOp.entrySet()) {
            String op = e.getKey();
            List<AbnormalSortingExcelReader.RowModel> list = e.getValue();

            long volume = list.stream()
                    .mapToLong(AbnormalSortingExcelReader.RowModel::getQty)
                    .sum();

            long skuCount = list.stream()
                    .map(AbnormalSortingExcelReader.RowModel::getSkuCode)
                    .filter(s -> s != null && !s.isBlank())
                    .distinct()
                    .count();

            LocalDateTime start = list.stream()
                    .map(AbnormalSortingExcelReader.RowModel::getTime)
                    .filter(Objects::nonNull)
                    .min(LocalDateTime::compareTo)
                    .orElse(null);

            LocalDateTime end = list.stream()
                    .map(AbnormalSortingExcelReader.RowModel::getTime)
                    .filter(Objects::nonNull)
                    .max(LocalDateTime::compareTo)
                    .orElse(null);

            // Hassas dakika (Saniyeler dahil)
            double mins = minutesBetweenPrecise(start, end);

            // İstediğin HH:mm:ss formatı
            String hhmmss = formatDuration(start, end);

            // Kesin UPH hesaplaması
            long uph = calcUph(volume, mins);

            operatorRows.add(new OperatorRowDto(op, skuCount, hhmmss, uph));
        }

        // UPH'ye göre azalan sıralama
        operatorRows.sort(Comparator.comparingLong(OperatorRowDto::getUph).reversed());

        // -------------------------
        // TOP PERFORMERS
        // -------------------------
        List<OperatorRowDto> topByUph = operatorRows.stream()
                .sorted(Comparator.comparingLong(OperatorRowDto::getUph).reversed())
                .limit(3)
                .toList();

        List<OperatorRowDto> topBySku = operatorRows.stream()
                .sorted(Comparator.comparingLong(OperatorRowDto::getSku).reversed())
                .limit(3)
                .toList();

        // -------------------------
        // METRICS UPDATE
        // -------------------------
        int abnormalSessionSku = (int) rows.stream()
                .map(AbnormalSortingExcelReader.RowModel::getSkuCode)
                .filter(s -> s != null && !s.isBlank())
                .distinct()
                .count();

        metricsService.updateAbnormalSku(metricsService.today(), abnormalSessionSku);

        return new AbnormalSortingResponse(
                new SessionStatsDto(sessionVolume, sessionAvgUph),
                new TopPerformersDto(topByUph, topBySku),
                operatorRows
        );
    }

    /**
     * FORMÜL: UPH = (Birim / Dakika) * 60
     * Saniyeleri double olarak işlediğimiz için sonuç çok hassastır.
     */
    private long calcUph(long volume, double minutesWorked) {
        if (minutesWorked <= 0) return 0;
        return Math.round((volume * 60.0) / minutesWorked);
    }

    /**
     * Saniye bazlı kesin dakika hesaplama.
     * Örnek: 6 dk 28 sn = 6.4667 dakika döner.
     */
    private double minutesBetweenPrecise(LocalDateTime a, LocalDateTime b) {
        if (a == null || b == null) return 0.0;
        long seconds = Duration.between(a, b).getSeconds();
        if (seconds <= 0) return 0.0;
        return seconds / 60.0;
    }

    /**
     * Süreyi HH:mm:ss formatında döndürür.
     */
    private String formatDuration(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) return "00:00:00";
        Duration duration = Duration.between(start, end);
        long seconds = Math.abs(duration.getSeconds());
        long h = seconds / 3600;
        long m = (seconds % 3600) / 60;
        long s = seconds % 60;
        return String.format("%02d:%02d:%02d", h, m, s);
    }

    private String safeStr(String s) {
        if (s == null) return "UNKNOWN";
        String t = s.trim();
        return t.isEmpty() ? "UNKNOWN" : t;
    }
}