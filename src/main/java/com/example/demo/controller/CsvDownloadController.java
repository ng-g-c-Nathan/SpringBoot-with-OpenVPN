package com.example.demo.controller;

import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.springframework.beans.factory.annotation.Value;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Controlador CsvDownloadController
 * * Gestiona la descarga segura de reportes de tráfico en formato CSV.
 * Implementa transferencia de datos por streaming para optimizar el uso de memoria
 * del servidor al manejar archivos potencialmente pesados.
 */
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/csv_files")
public class CsvDownloadController {

    // --- CONFIGURACIÓN DE DIRECTORIOS ---

    /** Directorio raíz donde se almacenan los reportes de tráfico (inyectado por config) */
    @Value("${traffic.dir}")
    private String TRAFFIC_DIR;

    /**
     * DTO para la solicitud de descarga.
     * Contiene el nombre del archivo específico generado previamente.
     */
    public static class CsvRequest {
        private String CSVFILE;

        public String getCSVFILE() { return CSVFILE; }
        public void setCSVFILE(String CSVFILE) { this.CSVFILE = CSVFILE; }
    }

    // --- ENDPOINTS DE DESCARGA ---

    /**
     * Procesa la descarga de un archivo CSV específico.
     * * Incluye validaciones de seguridad para prevenir ataques de navegación de directorios
     * y utiliza un buffer de salida para una transferencia eficiente.
     *
     * @param request Objeto que contiene el nombre del archivo solicitado.
     * @return ResponseEntity con el flujo de datos (StreamingResponseBody).
     */
    @PostMapping("/download")
    public ResponseEntity<StreamingResponseBody> downloadCsv(
            @RequestBody(required = false) CsvRequest request
    ) {

        // --- VALIDACIÓN DE ENTRADA Y SEGURIDAD ---

        if (request == null || request.getCSVFILE() == null || request.getCSVFILE().isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        String filename = request.getCSVFILE();

        /** * Seguridad: Sanitización básica contra Path Traversal.
         * Evita que un usuario malintencionado acceda a archivos fuera de TRAFFIC_DIR usando "../".
         */
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            return ResponseEntity.badRequest().build();
        }

        Path filePath = Paths.get(TRAFFIC_DIR).resolve(filename);

        if (!Files.exists(filePath)) {
            return ResponseEntity.notFound().build();
        }

        // --- LÓGICA DE STREAMING (TRANSFERENCIA) ---

        /**
         * Definición del cuerpo de respuesta por streaming.
         * Se utiliza un buffer de 64KB para maximizar el throughput de red.
         */
        StreamingResponseBody stream = outputStream -> {
            byte[] buffer = new byte[64 * 1024];

            try (InputStream in = Files.newInputStream(filePath)) {
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    // Asegura que los fragmentos de datos se envíen de inmediato
                    outputStream.flush();
                }
            }
        };

        // --- CONSTRUCCIÓN DE CABECERAS HTTP ---

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(stream);
    }
}