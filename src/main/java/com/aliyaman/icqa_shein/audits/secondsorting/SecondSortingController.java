package com.aliyaman.icqa_shein.audits.secondsorting;


import com.aliyaman.icqa_shein.audits.secondsorting.dto.AnalyzeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/audits/second-sorting")
@RequiredArgsConstructor
public class SecondSortingController {

    private final SecondSortingService service;

    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AnalyzeResponse> analyze(
            @RequestPart("waveFile") MultipartFile waveFile,
            @RequestPart("exceptionFile") MultipartFile exceptionFile,
            @RequestParam(value = "shiftType", required = false) String shiftType,
            @RequestParam(value = "shiftDate", required = false) String shiftDate
    ) {
        if (shiftType != null || shiftDate != null) {
            return ResponseEntity.ok(service.analyze(waveFile, exceptionFile, shiftType, shiftDate));
        }
        return ResponseEntity.ok(service.analyze(waveFile, exceptionFile));
    }
}
