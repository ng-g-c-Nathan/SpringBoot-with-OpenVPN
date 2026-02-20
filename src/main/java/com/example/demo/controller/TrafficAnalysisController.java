package com.example.demo.controller;

import com.example.demo.domain.TrainRequest;
import com.example.demo.service.AnalysisService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.Map;

/**
 * Controlador TrafficAnalysisController
 * * Orquestador de procesos de Machine Learning aplicados al tráfico de red.
 * Gestiona la ejecución de análisis (scoring) sobre archivos CSV, el entrenamiento
 * de modelos predictivos y la exportación de historiales y logs técnicos.
 */
@RestController
@RequestMapping("/api/traffic")
@CrossOrigin(origins = "*")
public class TrafficAnalysisController {

    // --- DEPENDENCIAS ---

    /** Servicio que encapsula la lógica de ejecución de scripts de análisis y entrenamiento */
    private final AnalysisService analysisService;

    /**
     * Inyección del servicio de análisis.
     * @param analysisService Lógica de integración con los modelos de IA.
     */
    public TrafficAnalysisController(AnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    // --- ACCIONES DE ANÁLISIS (SCORING) ---

    /**
     * Ejecuta el análisis de detección sobre un archivo CSV específico.
     * * Solicita al servicio procesar los datos para identificar posibles anomalías.
     * * @param request DTO que contiene el nombre del archivo CSV a analizar.
     * @return 200 OK si el análisis inició correctamente o 500 en caso de fallo.
     */
    @PostMapping("/score")
    public ResponseEntity<?> run(@RequestBody CsvDownloadController.CsvRequest request) {

        if (request == null || request.getCSVFILE() == null) {
            return ResponseEntity.badRequest().body("El nombre del archivo CSV es requerido");
        }

        try {
            analysisService.runAnalysis(request.getCSVFILE(), request.getRange());
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Error al ejecutar el análisis: " + e.getMessage());
        }
    }

    // --- GESTIÓN DE MODELOS Y ENTRENAMIENTO ---

    /**
     * Inicia el proceso de re-entrenamiento de los modelos de detección.
     * * Permite definir el modo de entrenamiento y el rango de fechas de los datos.
     * * @param request Objeto con parámetros de configuración para el entrenamiento.
     * @return Confirmación de recepción de la solicitud.
     */
    @PostMapping("/train")
    public ResponseEntity<?> train(@RequestBody TrainRequest request) {

        if (request == null || request.getMode() == null || request.getMode().isBlank()) {
            return ResponseEntity.badRequest().body("El campo 'mode' es requerido");
        }

        try {
            analysisService.runTraining(
                    request.getMode(),
                    request.getFromDate(),
                    request.getToDate()
            );

            return ResponseEntity.ok().body(
                    Map.of("status", "received", "message", "Proceso de entrenamiento iniciado")
            );

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    // --- EXPORTACIÓN DE RESULTADOS Y LOGS ---

    /**
     * Descarga el archivo JSON con el historial completo de análisis realizados.
     * @return Recurso de sistema de archivos (analysis_history.json).
     */
    @GetMapping("/history")
    public ResponseEntity<Resource> history() {
        try {
            File file = analysisService.getHistoryFile();
            Resource resource = new FileSystemResource(file);

            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=\"analysis_history.json\"")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Obtiene metadatos técnicos sobre los modelos cargados actualmente.
     * @return String en formato JSON con la información de los modelos.
     */
    @GetMapping("/models_info")
    public ResponseEntity<?> modelsInfo() {
        try {
            String json = analysisService.getAllModelInfo();
            return ResponseEntity.ok()
                    .header("Content-Type", "application/json")
                    .body(json);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    /**
     * Descarga el log detallado del último proceso de entrenamiento.
     * @return Archivo JSON con la trazabilidad del entrenamiento.
     */
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
}