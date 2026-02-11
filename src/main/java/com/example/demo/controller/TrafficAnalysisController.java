package com.example.demo.controller;


import com.example.demo.service.AnalysisService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.Map;

@RestController
@RequestMapping("/api/traffic")
@CrossOrigin(origins = "*")
public class TrafficAnalysisController {

    private final AnalysisService analysisService;

    public TrafficAnalysisController(AnalysisService analysisService) {
        this.analysisService = analysisService;
    }


    @PostMapping("/score")
    public ResponseEntity<?> run(@RequestBody CsvDownloadController.CsvRequest request) {

        if (request == null || request.getCSVFILE() == null) {
            return ResponseEntity.badRequest().body("CSVFILE requerido");
        }

        try {
            analysisService.runAnalysis(request.getCSVFILE());
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(e.getMessage());
        }
    }


    @GetMapping("/history")
    public ResponseEntity<Resource> history() {

        try {
            File file = analysisService.getHistoryFile();

            Resource resource = new FileSystemResource(file);

            return ResponseEntity.ok()
                    .header("Content-Disposition",
                            "attachment; filename=\"analysis_history.json\"")
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}
