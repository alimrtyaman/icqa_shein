package com.aliyaman.icqa_shein.audits.abnormalsorting;

import com.aliyaman.icqa_shein.audits.abnormalsorting.dto.AbnormalSortingResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/audits/abnormal-sorting")
@RequiredArgsConstructor
public class AbnormalSortingController {

    private final AbnormalSortingService service;

    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AbnormalSortingResponse> analyze(@RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(service.analyze(file));
    }
}