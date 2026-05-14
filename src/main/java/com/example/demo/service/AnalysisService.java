package com.example.demo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Servicio AnalysisService
 *
 * Delega la ejecución del pipeline de ML a la API Flask (python-scorer).
 * Todo va via HTTP — sin procesos locales ni archivos compartidos.
 */
@Service
public class AnalysisService {

    @Value("${python.api.url:http://172.17.0.1:5000}")
    private String PYTHON_API_URL;

    private final RestTemplate restTemplate = new RestTemplate();

    // --- ANÁLISIS (SCORING) ---

    public void runAnalysis(String filename, String range) throws Exception {

        if (!filename.toLowerCase().endsWith(".csv")) {
            filename += ".csv";
        }

        Map<String, String> body = new HashMap<>();
        body.put("csv_file", filename);
        body.put("range", range != null ? range : "global");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);

        try {
            restTemplate.postForEntity(PYTHON_API_URL + "/score", entity, String.class);
        } catch (Exception e) {
            throw new RuntimeException("Error llamando a Flask /score: " + e.getMessage());
        }
    }

    // --- HISTORIAL ---

    public File getHistoryFile() {
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(
                    PYTHON_API_URL + "/history", String.class);
            File tmp = File.createTempFile("analysis_history", ".json");
            tmp.deleteOnExit();
            java.nio.file.Files.writeString(tmp.toPath(), response.getBody());
            return tmp;
        } catch (Exception e) {
            throw new RuntimeException("Historial no disponible: " + e.getMessage());
        }
    }

    // --- MODELOS INFO ---

    public String getAllModelInfo() throws Exception {
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(
                    PYTHON_API_URL + "/models_info", String.class);
            return response.getBody();
        } catch (Exception e) {
            throw new RuntimeException("Error llamando a Flask /models_info: " + e.getMessage());
        }
    }

    // --- TRAINING LOG ---

    public File getTrainingLogFile() {
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(
                    PYTHON_API_URL + "/training_log", String.class);
            File tmp = File.createTempFile("training_log", ".json");
            tmp.deleteOnExit();
            java.nio.file.Files.writeString(tmp.toPath(), response.getBody());
            return tmp;
        } catch (Exception e) {
            throw new RuntimeException("Log de entrenamiento no disponible: " + e.getMessage());
        }
    }

    // --- ENTRENAMIENTO ---

    public void runTraining(String mode, String fromDate, String toDate) throws Exception {

        if (mode == null || mode.isBlank()) {
            throw new RuntimeException("El modo de entrenamiento es obligatorio.");
        }

        Map<String, String> body = new HashMap<>();
        body.put("mode", mode);
        if (fromDate != null && !fromDate.isBlank()) body.put("fromDate", fromDate);
        if (toDate   != null && !toDate.isBlank())   body.put("toDate",   toDate);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);

        try {
            restTemplate.postForEntity(PYTHON_API_URL + "/train", entity, String.class);
        } catch (Exception e) {
            throw new RuntimeException("Error llamando a Flask /train: " + e.getMessage());
        }
    }
}