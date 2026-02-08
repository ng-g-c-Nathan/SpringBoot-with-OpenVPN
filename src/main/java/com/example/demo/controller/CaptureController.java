package com.example.demo.controller;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/capture")
public class CaptureController {

    @Value("${script.path}")
    private String SCRIPT_PATH;

    public static class CaptureRequest {
        private String duration;

        public String getDuration() {
            return duration;
        }

        public void setDuration(String duration) {
            this.duration = duration;
        }
    }

    @PostMapping
    public ResponseEntity<?> startCapture(@RequestBody(required = false) CaptureRequest request) {

        String durationStr = "10";

        if (request != null && request.getDuration() != null) {
            durationStr = request.getDuration();
        }

        int duration;
        try {
            duration = Integer.parseInt(durationStr);
        } catch (NumberFormatException e) {
            return ResponseEntity
                    .badRequest()
                    .body(Map.of("error", "La duración debe ser un número entero válido"));
        }

        Path script = Path.of(SCRIPT_PATH);

        if (!Files.exists(script)) {
            return ResponseEntity
                    .status(500)
                    .body(Map.of("error", "Script no encontrado"));
        }

        try {

            ProcessBuilder pb = new ProcessBuilder(
                    SCRIPT_PATH,
                    String.valueOf(duration)
            );

            pb.redirectErrorStream(true);

            Process p = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader br =
                         new BufferedReader(new InputStreamReader(p.getInputStream()))) {

                String line;
                while ((line = br.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            int exitCode = p.waitFor();

            if (exitCode != 0) {
                return ResponseEntity
                        .status(500)
                        .body(Map.of("error", "Fallo en ejecución", "output", output.toString()));
            }

            return ResponseEntity.ok(
                    Map.of(
                            "message", "Captura iniciada por " + duration + " segundos",
                            "output", output.toString()
                    )
            );

        } catch (Exception e) {
            return ResponseEntity
                    .status(500)
                    .body(Map.of("error", "Error inesperado: " + e.getMessage()));
        }
    }
}
