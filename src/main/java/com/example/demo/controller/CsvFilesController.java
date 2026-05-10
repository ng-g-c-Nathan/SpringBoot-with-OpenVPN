package com.example.demo.controller;

import com.example.demo.domain.PcapInfo;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Value;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Controlador CsvFilesController
 *
 * Gestiona el inventario de capturas de red (.pcap) y sus reportes (.csv).
 * Si no existe ningún .pcap en el directorio, lista directamente los .csv
 * disponibles (modo fallback).
 */
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/csv_files")
public class CsvFilesController {

    @Value("${traffic.dir}")
    private String TRAFFIC_DIR;

    @Value("${reg.script.path}")
    private String REG_DIR;

    // -------------------------------------------------------------------
    // MÉTODOS DE SOPORTE
    // -------------------------------------------------------------------

    /**
     * Busca el CSV generado que corresponde a un PCAP dado.
     */
    private Path findGeneratedCsv(Path dir, String pcapFilename) throws IOException {
        String base = pcapFilename.replaceFirst("\\.pcap$", "");
        try (Stream<Path> s = Files.list(dir)) {
            return s.filter(p -> p.getFileName().toString().startsWith(base))
                    .filter(p -> p.getFileName().toString().endsWith(".csv"))
                    .findFirst()
                    .orElse(null);
        }
    }

    /**
     * Extrae los minutos de captura embebidos en el nombre del archivo.
     * Formato esperado: "nombre_(X_minutes).pcap"
     */
    private double extractMinutesFromFilename(String filename) {
        Pattern p = Pattern.compile("\\((\\d+(?:\\.\\d+)?)_minutes\\)");
        Matcher m = p.matcher(filename);
        return m.find() ? Double.parseDouble(m.group(1)) : -1;
    }

    /**
     * Determina el estado de procesamiento de un PCAP:
     * - "true"    → CSV ya existe
     * - "pending" → aún dentro del tiempo esperado
     * - "false"   → tiempo excedido sin CSV
     */
    private String resolveCsvStatus(Path pcapPath, Path csvDir) throws IOException {
        Path csv = findGeneratedCsv(csvDir, pcapPath.getFileName().toString());
        if (csv != null) return "true";

        double minutes = extractMinutesFromFilename(pcapPath.getFileName().toString());
        if (minutes <= 0) return "pending";

        long tripleMillis = (long) (minutes * 3 * 60_000);
        long lastModified = Files.getLastModifiedTime(pcapPath).toMillis();

        return (System.currentTimeMillis() - lastModified > tripleMillis) ? "false" : "pending";
    }

    // -------------------------------------------------------------------
    // ENDPOINTS
    // -------------------------------------------------------------------

    /**
     * Lista archivos del directorio de tráfico.
     *
     * Comportamiento:
     *   1. Si hay archivos .pcap → lista PCAPs con su estado de procesamiento.
     *   2. Si NO hay ningún .pcap → lista los .csv directamente (modo fallback).
     *
     * En el modo fallback, el campo csvFile contiene el nombre del .csv y
     * el status siempre es "true" (el CSV ya existe por definición).
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<PcapInfo>> listFiles() {
        Path dirPath = Paths.get(TRAFFIC_DIR);

        if (!Files.exists(dirPath) || !Files.isDirectory(dirPath)) {
            return ResponseEntity.status(404).build();
        }

        try {
            // Contar cuántos PCAPs hay en el directorio
            long pcapCount;
            try (Stream<Path> s = Files.list(dirPath)) {
                pcapCount = s.filter(p -> p.toString().endsWith(".pcap")).count();
            }

            if (pcapCount > 0) {
                // ── MODO NORMAL: listar PCAPs ──
                List<PcapInfo> list = Files.list(dirPath)
                        .filter(p -> p.toString().endsWith(".pcap"))
                        .sorted((a, b) -> {
                            try {
                                return Long.compare(
                                        Files.getLastModifiedTime(b).toMillis(),
                                        Files.getLastModifiedTime(a).toMillis());
                            } catch (IOException e) { return 0; }
                        })
                        .map(path -> {
                            try {
                                File file = path.toFile();
                                Path csv   = findGeneratedCsv(dirPath, file.getName());
                                String status = resolveCsvStatus(path, dirPath);
                                return new PcapInfo(
                                        file.getName(),
                                        Files.getLastModifiedTime(path).toInstant(),
                                        file.length(),
                                        status,
                                        csv != null ? csv.getFileName().toString() : null
                                );
                            } catch (Exception e) { return null; }
                        })
                        .filter(info -> info != null)
                        .toList();

                return ResponseEntity.ok(list);

            } else {
                // ── MODO FALLBACK: no hay PCAPs, listar CSVs directamente ──
                List<PcapInfo> list = Files.list(dirPath)
                        .filter(p -> p.toString().endsWith(".csv"))
                        .sorted((a, b) -> {
                            try {
                                return Long.compare(
                                        Files.getLastModifiedTime(b).toMillis(),
                                        Files.getLastModifiedTime(a).toMillis());
                            } catch (IOException e) { return 0; }
                        })
                        .map(path -> {
                            try {
                                File file = path.toFile();
                                String csvName = file.getName();
                                return new PcapInfo(
                                        csvName,                                    // name (el CSV hace de PCAP)
                                        Files.getLastModifiedTime(path).toInstant(),
                                        file.length(),
                                        "true",                                     // ya existe, siempre true
                                        csvName                                     // csvFile apunta al mismo
                                );
                            } catch (Exception e) { return null; }
                        })
                        .filter(info -> info != null)
                        .toList();

                return ResponseEntity.ok(list);
            }

        } catch (IOException e) {
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Dispara el re-procesamiento de un PCAP específico (fire & forget).
     */
    @PostMapping("/reparar/{filename:.+}")
    public ResponseEntity<?> reparar(@PathVariable String filename) {
        Path pcapPath = Paths.get(TRAFFIC_DIR).resolve(filename);

        if (!Files.exists(pcapPath)) {
            return ResponseEntity.notFound().build();
        }

        try {
            String serverIp = "10.0.0.8";

            ProcessBuilder pb = new ProcessBuilder("python3", REG_DIR, pcapPath.toString(), serverIp);
            pb.redirectErrorStream(true);
            pb.start();

            return ResponseEntity.accepted().build();

        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error ejecutando script: " + e.getMessage());
        }
    }
}