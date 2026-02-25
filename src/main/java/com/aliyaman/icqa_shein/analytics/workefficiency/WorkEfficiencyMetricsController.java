package com.aliyaman.icqa_shein.analytics.workefficiency;


import com.aliyaman.icqa_shein.analytics.workefficiency.dto.WorkEfficiencyMetricsPointDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/work-efficiency/metrics")
@RequiredArgsConstructor
public class WorkEfficiencyMetricsController {

    private final WorkEfficiencyMetricsService service;

    // GET /api/work-efficiency/metrics?start=2026-02-01&end=2026-02-24
    @GetMapping
    public List<WorkEfficiencyMetricsPointDto> getMetrics(
            @RequestParam String start,
            @RequestParam String end
    ) {
        return service.getRange(LocalDate.parse(start), LocalDate.parse(end));
    }

    // POST /api/work-efficiency/metrics/debug-today
    // { "inventory": 10, "abnormal": 3 }
    @PostMapping("/debug-today")
    public String debugToday(@RequestBody Map<String, Integer> body) {
        LocalDate today = service.today();

        Integer abnormal = body.get("abnormal");
        Integer inventory = body.get("inventory");

        if (abnormal != null) service.updateAbnormalSku(today, abnormal);
        if (inventory != null) service.updateInventorySku(today, inventory);

        return "OK saved for " + today;
    }
}