package com.example.demo.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Controlador CaptureController
 * * Se encarga de la orquestación y ejecución de scripts externos para la captura de tráfico.
 * Actúa como puente entre las peticiones HTTP de la interfaz y el sistema operativo,
 * gestionando la validación de parámetros y el flujo de salida del proceso.
 */
@RestController
@RequestMapping("/api/capture")
public class CaptureController {

    // --- CONFIGURACIÓN Y DEPENDENCIAS ---

    /** Ruta absoluta del script de captura definida en application.properties */
    @Value("${script.path}")
    private String SCRIPT_PATH;

    /**
     * DTO (Data Transfer Object) para las solicitudes de captura.
     * Define la estructura esperada en el cuerpo del JSON.
     */
    public static class CaptureRequest {
        private String duration;

        public String getDuration() {
            return duration;
        }

        public void setDuration(String duration) {
            this.duration = duration;
        }
    }

    // --- ENDPOINTS DE CAPTURA ---

    /**
     * Inicia un proceso de captura de tráfico mediante la ejecución de un script local.
     * * El método valida la existencia del script, parsea la duración solicitada
     * y captura la salida estándar (stdout/stderr) para retornarla al cliente.
     *
     * @param request Objeto que contiene la duración opcional de la captura.
     * @return ResponseEntity con el resultado de la ejecución o mensajes de error detallados.
     */
    @PostMapping
    public ResponseEntity<?> startCapture(@RequestBody(required = false) CaptureRequest request) {

        // --- VALIDACIÓN DE PARÁMETROS ---

        String durationStr = "10"; // Valor por defecto

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

        // --- VERIFICACIÓN DE ENTORNO ---

        Path script = Path.of(SCRIPT_PATH);
        if (!Files.exists(script)) {
            return ResponseEntity
                    .status(500)
                    .body(Map.of("error", "Script no encontrado en la ruta: " + SCRIPT_PATH));
        }

        // --- LÓGICA DE EJECUCIÓN DE PROCESO ---

        try {
            ProcessBuilder pb = new ProcessBuilder(
                    SCRIPT_PATH,
                    String.valueOf(duration)
            );

            // Redirige el error a la salida estándar para capturarlo en un solo flujo
            pb.redirectErrorStream(true);

            Process p = pb.start();

            // Lectura de la consola del script
            StringBuilder output = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            // Espera la finalización del proceso externo
            int exitCode = p.waitFor();

            if (exitCode != 0) {
                return ResponseEntity
                        .status(500)
                        .body(Map.of("error", "Fallo en ejecución", "exitCode", exitCode, "output", output.toString()));
            }

            return ResponseEntity.ok(
                    Map.of(
                            "message", "Captura finalizada exitosamente por " + duration + " segundos",
                            "output", output.toString()
                    )
            );

        } catch (Exception e) {
            // Manejo de excepciones de Interrupción o I/O
            return ResponseEntity
                    .status(500)
                    .body(Map.of("error", "Error inesperado en el servidor: " + e.getMessage()));
        }
    }
}