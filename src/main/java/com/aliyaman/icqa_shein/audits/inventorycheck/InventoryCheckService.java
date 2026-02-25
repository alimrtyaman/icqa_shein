package com.aliyaman.icqa_shein.audits.inventorycheck;

import com.aliyaman.icqa_shein.analytics.workefficiency.WorkEfficiencyMetricsService;
import com.aliyaman.icqa_shein.audits.inventorycheck.dto.*;
import com.aliyaman.icqa_shein.audits.inventorycheck.excel.InventoryCheckExcelReader;
import com.aliyaman.icqa_shein.audits.inventorycheck.excel.InventoryCheckExcelReader.RowModel;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;



import org.springframework.web.multipart.MultipartFile;

import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;



import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
        import java.util.stream.Collectors;
@Service
public class InventoryCheckService {

    private final InventoryCheckExcelReader reader;
    private final WorkEfficiencyMetricsService metricsService;

    public InventoryCheckService(InventoryCheckExcelReader reader,
                                 WorkEfficiencyMetricsService metricsService) {
        this.reader = reader;
        this.metricsService = metricsService;
    }

    public InventoryCheckResponse analyze(MultipartFile file) {
        var parsed = reader.read(file);
        var rows = parsed.getRows();

        long totalRows = rows.size();

        Set<String> allLocations = new HashSet<>();
        Set<String> allSkus = new HashSet<>();

        // ✅ THIS is the correct session discrepancy for your file:
        // Discrepancies = | SUM(Differences) |
        long sumDifferencesSigned = 0;

        // Keep varianceUnits as "how much error movement overall" (row abs sum)
        long varianceUnits = 0;

        // For accuracy: count rows where Differences != 0
        long discrepancyRows = 0;

        Map<String, OpAgg> ops = new HashMap<>();

        for (InventoryCheckExcelReader.RowModel r : rows) {
            if (!blank(r.getLocation())) allLocations.add(r.getLocation());
            if (!blank(r.getSku())) allSkus.add(r.getSku());

            long diff = r.getDifferences();          // already system - operator
            long absDiff = Math.abs(diff);

            sumDifferencesSigned += diff;
            varianceUnits += absDiff;
            if (diff != 0) discrepancyRows++;

            String opKey = blank(r.getOperator()) ? "UNKNOWN" : r.getOperator();
            OpAgg agg = ops.computeIfAbsent(opKey, k -> new OpAgg());

            agg.rows++;
            if (!blank(r.getLocation())) agg.locations.add(r.getLocation());
            if (!blank(r.getSku())) agg.skus.add(r.getSku());

            // ✅ Operator discrepancy should also be NET: |SUM(diff)| (not sum abs)
            agg.sumDifferencesSigned += diff;

            // keep also row-based for accuracy
            if (diff != 0) agg.discrepancyRows++;

            LocalDateTime t = r.getCollectionTime();
            if (t != null) {
                if (agg.min == null || t.isBefore(agg.min)) agg.min = t;
                if (agg.max == null || t.isAfter(agg.max)) agg.max = t;
            }
        }

        long discrepancies = Math.abs(sumDifferencesSigned);

        double accuracyPct = totalRows == 0
                ? 0
                : round2((totalRows - discrepancyRows) * 100.0 / totalRows);

        List<OperatorRowDto> operatorRows = ops.entrySet().stream()
                .map(e -> toDto(e.getKey(), e.getValue()))
                .sorted(Comparator.comparingLong(OperatorRowDto::getBph).reversed())
                .collect(Collectors.toList());

        long maxBph = operatorRows.stream()
                .mapToLong(OperatorRowDto::getBph)
                .max()
                .orElse(0);

        long avgBph = Math.round(operatorRows.stream()
                .mapToLong(OperatorRowDto::getBph)
                .average()
                .orElse(0.0));

        SessionStatsDto session = SessionStatsDto.builder()
                .totalRows(totalRows)
                .totalLocations(allLocations.size())
                .totalSku(allSkus.size())
                .discrepancies(discrepancies)     // ✅ |SUM(Differences)|
                .accuracyPct(accuracyPct)
                .varianceUnits(varianceUnits)     // ✅ SUM(|Differences|)
                .avgBph(avgBph)
                .maxBph(maxBph)
                .build();

        // ✅ SAVE DAILY METRIC (inventorySku = unique session SKU count)
        metricsService.updateInventorySku(metricsService.today(), allSkus.size());

        List<OperatorRowDto> topByBph = operatorRows.stream().limit(3).toList();
        List<OperatorRowDto> topByLocations = operatorRows.stream()
                .sorted(Comparator.comparingLong(OperatorRowDto::getLocations).reversed())
                .limit(3)
                .toList();

        TopPerformersDto top = TopPerformersDto.builder()
                .topByBph(topByBph)
                .topByLocations(topByLocations)
                .build();

        return InventoryCheckResponse.builder()
                .sessionStats(session)
                .topPerformers(top)
                .operators(operatorRows)
                .build();
    }

    private OperatorRowDto toDto(String operator, OpAgg agg) {
        long loc = agg.locations.size();
        long sku = agg.skus.size();

        Duration dur = Duration.ZERO;
        if (agg.min != null && agg.max != null && agg.max.isAfter(agg.min)) {
            dur = Duration.between(agg.min, agg.max);
        }

        double hours = Math.max(1.0 / 3600.0, dur.toSeconds() / 3600.0);
        long bph = Math.round(loc / hours);

        // ✅ accuracy row-based (differences != 0 olan satırlar hata)
        double acc = agg.rows == 0
                ? 0
                : round2((agg.rows - agg.discrepancyRows) * 100.0 / agg.rows);

        // ✅ operator discrepancies = |SUM(Differences for operator)|
        long operatorDiscrepancies = Math.abs(agg.sumDifferencesSigned);

        return OperatorRowDto.builder()
                .operator(operator)
                .locations(loc)
                .sku(sku)
                .time(formatHms(dur))
                .bph(bph)
                .discrepancies(operatorDiscrepancies)
                .accuracyPct(acc)
                .build();
    }

    private String formatHms(Duration d) {
        long sec = Math.max(0, d.toSeconds());
        long h = sec / 3600;
        long m = (sec % 3600) / 60;
        long s = sec % 60;
        return String.format("%02d:%02d:%02d", h, m, s);
    }

    private boolean blank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private static class OpAgg {
        long rows = 0;

        // ✅ NET signed sum of Differences for the operator
        long sumDifferencesSigned = 0;

        // ✅ row-based for accuracy
        long discrepancyRows = 0;

        Set<String> locations = new HashSet<>();
        Set<String> skus = new HashSet<>();
        LocalDateTime min;
        LocalDateTime max;
    }
}