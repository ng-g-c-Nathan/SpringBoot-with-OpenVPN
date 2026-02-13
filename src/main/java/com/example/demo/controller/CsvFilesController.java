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
 * * Gestiona el inventario de capturas de red (.pcap) y sus reportes procesados (.csv).
 * Este controlador es responsable de calcular el estado de procesamiento (pending, true, false)
 * basándose en el tiempo de vida de los archivos y coordinar la reparación/re-procesamiento.
 */
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/csv_files")
public class CsvFilesController {

    // --- CONFIGURACIÓN DE RUTAS ---

    /** Directorio donde se almacenan capturas y reportes */
    @Value("${traffic.dir}")
    private String TRAFFIC_DIR;

    /** Ruta del script de Python encargado de procesar/reparar los archivos */
    @Value("${reg.script.path}")
    private String REG_DIR;

    // --- MÉTODOS DE SOPORTE (LÓGICA INTERNA) ---

    /**
     * Busca un archivo CSV generado que coincida con el nombre base de un PCAP.
     * @param dir Directorio de búsqueda.
     * @param pcapFilename Nombre del archivo de captura original.
     * @return Path del CSV encontrado o null si no existe.
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
     * Extrae el tiempo de captura (minutos) embebido en el nombre del archivo.
     * Espera el formato: "nombre(valor_minutes).pcap"
     * @return Valor numérico de minutos o -1 si no hay coincidencia.
     */
    private double extractMinutesFromFilename(String filename) {
        Pattern p = Pattern.compile("\\((\\d+(?:\\.\\d+)?)_minutes\\)");
        Matcher m = p.matcher(filename);
        if (m.find()) {
            return Double.parseDouble(m.group(1));
        }
        return -1;
    }

    /**
     * Determina el estado de procesamiento de un archivo PCAP.
     * * Lógica de estados:
     * - 'true': El CSV ya existe.
     * - 'pending': El proceso aún está en tiempo esperado de ejecución.
     * - 'false': El tiempo esperado excedió (3x duración) y el CSV no aparece.
     */
    private String resolveCsvStatus(Path pcapPath, Path csvDir) throws IOException {
        Path csv = findGeneratedCsv(csvDir, pcapPath.getFileName().toString());
        if (csv != null) return "true";

        String filename = pcapPath.getFileName().toString();
        double minutes = extractMinutesFromFilename(filename);

        if (minutes <= 0) return "pending";

        // Umbral de tolerancia: 3 veces la duración de la captura
        long tripleMillis = (long) (minutes * 3 * 60_000);
        long lastModified = Files.getLastModifiedTime(pcapPath).toMillis();
        long now = System.currentTimeMillis();

        return (now - lastModified > tripleMillis) ? "false" : "pending";
    }

    // --- ENDPOINTS PRINCIPALES ---

    /**
     * Lista todos los archivos PCAP del servidor ordenados por fecha de modificación.
     * * Calcula dinámicamente el estado de cada archivo para informar al frontend.
     * @return Lista de objetos {@link PcapInfo} con metadatos y estado.
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<PcapInfo>> listPcapFiles() {
        Path dirPath = Paths.get(TRAFFIC_DIR);

        if (!Files.exists(dirPath) || !Files.isDirectory(dirPath)) {
            return ResponseEntity.status(404).build();
        }

        try {
            List<PcapInfo> pcapList = Files.list(dirPath)
                    .filter(path -> path.toString().endsWith(".pcap"))
                    .sorted((a, b) -> {
                        try {
                            return Long.compare(Files.getLastModifiedTime(b).toMillis(),
                                    Files.getLastModifiedTime(a).toMillis());
                        } catch (IOException e) { return 0; }
                    })
                    .map(path -> {
                        try {
                            File file = path.toFile();
                            Path csv = findGeneratedCsv(dirPath, file.getName());
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

            return ResponseEntity.ok(pcapList);
        } catch (IOException e) {
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Dispara el re-procesamiento (reparación) de un archivo específico.
     * * Ejecuta un script externo de Python en modo asíncrono (Fire and Forget).
     * @param filename Nombre del archivo PCAP a procesar.
     * @return 202 Accepted indicando que la tarea ha comenzado.
     */
    @PostMapping("/reparar/{filename:.+}")
    public ResponseEntity<?> reparar(@PathVariable String filename) {
        Path pcapPath = Paths.get(TRAFFIC_DIR).resolve(filename);

        if (!Files.exists(pcapPath)) {
            return ResponseEntity.notFound().build();
        }

        try {
            // Dirección IP estática del servidor de destino para el análisis
            String serverIp = "10.0.0.8";

            ProcessBuilder pb = new ProcessBuilder(
                    "python3",
                    REG_DIR,
                    pcapPath.toString(),
                    serverIp
            );

            pb.redirectErrorStream(true);
            pb.start(); // Se inicia pero no se espera a su finalización (proceso en background)

            return ResponseEntity.accepted().build();

        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error ejecutando script de reparación: " + e.getMessage());
        }
    }
}