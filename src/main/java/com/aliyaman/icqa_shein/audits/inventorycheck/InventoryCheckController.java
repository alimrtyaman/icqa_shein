package com.aliyaman.icqa_shein.audits.inventorycheck;


import com.aliyaman.icqa_shein.audits.inventorycheck.dto.InventoryCheckResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/audits/inventory-check")
public class InventoryCheckController {

    private final InventoryCheckService service;

    public InventoryCheckController(InventoryCheckService service) {
        this.service = service;
    }

    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public InventoryCheckResponse analyze(@RequestPart("file") MultipartFile file) {
        return service.analyze(file);
    }
}