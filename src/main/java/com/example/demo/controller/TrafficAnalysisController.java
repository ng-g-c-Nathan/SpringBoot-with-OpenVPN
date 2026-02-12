package com.example.demo.controller;


import com.example.demo.domain.TrainRequest;
import com.example.demo.service.AnalysisService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

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

    @GetMapping("/models_info")
    public ResponseEntity<?> modelsInfo() {
        try {
            String json = analysisService.getAllModelInfo();
            return ResponseEntity.ok()
                    .header("Content-Type", "application/json")
                    .body(json);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(e.getMessage());
        }
    }

    @GetMapping("/training_log")
    public ResponseEntity<Resource> trainingLog() {
        try {
            File file = analysisService.getTrainingLogFile();
            Resource resource = new FileSystemResource(file);

            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=\"training_log.json\"")
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
    @PostMapping("/train")
    public ResponseEntity<?> train(@RequestBody TrainRequest request) {

        if (request == null || request.getMode() == null || request.getMode().isBlank()) {
            return ResponseEntity.badRequest().body("mode es requerido");
        }

        try {

            analysisService.runTraining(
                    request.getMode(),
                    request.getFromDate(),
                    request.getToDate()
            );

            // solo confirmación
            return ResponseEntity.ok().body(
                    Map.of("status", "received")
            );

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }



}
