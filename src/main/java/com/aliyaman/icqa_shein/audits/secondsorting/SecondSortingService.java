package com.aliyaman.icqa_shein.audits.secondsorting;

import com.aliyaman.icqa_shein.audits.secondsorting.dto.AnalyzeResponse;
import com.aliyaman.icqa_shein.audits.secondsorting.dto.SummaryDto;
import com.aliyaman.icqa_shein.audits.secondsorting.dto.WaveResultDto;
import com.aliyaman.icqa_shein.audits.secondsorting.excel.ExceptionListReader;
import com.aliyaman.icqa_shein.audits.secondsorting.excel.WaveAgg;
import com.aliyaman.icqa_shein.audits.secondsorting.excel.WaveListReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class SecondSortingService {

    private final WaveListReader waveReader;
    private final ExceptionListReader exceptionReader;

    public AnalyzeResponse analyze(MultipartFile waveFile, MultipartFile exceptionFile, String shiftType, String shiftDate) {
        // şimdilik shift paramlarını ignore edebilirsin
        return analyze(waveFile, exceptionFile);
    }

    public AnalyzeResponse analyze(MultipartFile waveFile, MultipartFile exceptionFile) {

        Map<String, LocalDateTime> waveEndTimeMap = waveReader.readWaveEndTimes(waveFile);
        Map<String, WaveAgg> agg = exceptionReader.readSecondSortingAgg(exceptionFile);

        List<WaveResultDto> results = new ArrayList<>();
        long missingEndTime = 0;

        for (var entry : agg.entrySet()) {
            String waveNo = entry.getKey();
            WaveAgg a = entry.getValue();

            LocalDateTime endTime = waveEndTimeMap.get(waveNo);
            LocalDateTime shelvingTime = a.maxShelvingTime();

            Double diffHours = null;
            if (endTime != null && shelvingTime != null) {
                long minutes = Duration.between(endTime, shelvingTime).toMinutes();
                diffHours = minutes / 60.0;
            } else if (endTime == null) {
                missingEndTime++;
            }

            results.add(new WaveResultDto(
                    waveNo,
                    a.count(),
                    endTime,
                    shelvingTime,
                    diffHours
            ));
        }

        results.sort(
                Comparator
                        .comparingLong(WaveResultDto::getExceptions)
                        .reversed()
                        .thenComparing(WaveResultDto::getTimeDiffHours,
                                Comparator.nullsLast(Comparator.reverseOrder()))
        );

        long totalExceptions = agg.values().stream().mapToLong(WaveAgg::count).sum();

        SummaryDto summary = new SummaryDto(
                totalExceptions,
                results.size(),
                missingEndTime
        );

        return new AnalyzeResponse(summary, results);
    }
}