package com.example.demo.service;

import com.example.demo.domain.TrafficDayStats;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Servicio CsvTrafficIndexService
 * * Administra un índice en memoria de las estadísticas de tráfico diarias.
 * Escanea el sistema de archivos al iniciar la aplicación, analiza los nombres de los
 * archivos CSV mediante expresiones regulares y consolida los datos para permitir
 * consultas rápidas de series temporales y rankings.
 */
@Service
public class CsvTrafficIndexService {

    // --- ESTRUCTURAS DE DATOS ---

    /** * Índice principal organizado por fecha.
     * Se usa TreeMap para garantizar que las claves (fechas) estén siempre ordenadas cronológicamente.
     */
    private final TreeMap<LocalDate, TrafficDayStats> index = new TreeMap<>();

    @Value("${traffic.dir}")
    private String trafficDir;

    /** * Patrón Regex para la extracción de metadatos del nombre del archivo.
     * Captura: 1. Fecha, 2. Valor Input, 3. Valor Output.
     * Ejemplo: traffic_2023-10-27_10-00-00_(...).csv
     */
    private static final Pattern FILE_PATTERN =
            Pattern.compile(
                    "traffic_(\\d{4}-\\d{2}-\\d{2})_\\d{2}-\\d{2}-\\d{2}_\\([^)]*\\)_\\(([^)]+)_input\\)_\\(([^)]+)_output\\)\\.csv"
            );

    // --- PROCESO DE INICIALIZACIÓN ---

    /**
     * Construye el índice de tráfico inmediatamente después de que el servicio es creado.
     * * Escanea el directorio configurado y procesa cada archivo CSV detectado.
     */
    @PostConstruct
    public void buildIndex() throws IOException {
        Path dir = Paths.get(trafficDir);
        if (!Files.exists(dir)) return;

        try (Stream<Path> stream = Files.list(dir)) {
            stream
                    .filter(p -> p.getFileName().toString().endsWith(".csv"))
                    .forEach(this::processFile);
        }
    }

    /**
     * Analiza un archivo individual y acumula sus valores en el índice diario.
     * * Si la fecha ya existe en el índice, suma el tráfico y aumenta el contador de capturas.
     * @param path Ruta del archivo CSV a procesar.
     */
    private void processFile(Path path) {
        String name = path.getFileName().toString();
        Matcher m = FILE_PATTERN.matcher(name);

        if (!m.matches()) return;

        LocalDate date = LocalDate.parse(m.group(1));
        double input = Double.parseDouble(m.group(2));
        double output = Double.parseDouble(m.group(3));

        // Operación atómica para actualizar o crear el registro diario
        index.compute(date, (k, v) -> {
            if (v == null)
                return new TrafficDayStats(k, input, output, 1);

            v.setTotalInput(v.getTotalInput() + input);
            v.setTotalOutput(v.getTotalOutput() + output);
            v.setCaptures(v.getCaptures() + 1);
            return v;
        });
    }

    // --- MÉTODOS DE CONSULTA ---

    /**
     * Obtiene una serie temporal de los últimos N días registrados.
     * @param days Cantidad de días hacia atrás desde el último registro.
     * @return Lista de estadísticas ordenadas por fecha.
     */
    public List<TrafficDayStats> lastDays(int days) {
        if (index.isEmpty()) return List.of();

        LocalDate to = index.lastKey();
        LocalDate from = to.minusDays(days - 1);

        return index.subMap(from, true, to, true)
                .values()
                .stream()
                .toList();
    }

    /**
     * Filtra el tráfico dentro de un rango de fechas específico.
     * @param from Fecha inicial (inclusive).
     * @param to Fecha final (inclusive).
     * @return Lista de estadísticas en el rango dado.
     */
    public List<TrafficDayStats> between(LocalDate from, LocalDate to) {
        if (index.isEmpty()) return List.of();

        return index.subMap(from, true, to, true)
                .values()
                .stream()
                .toList();
    }

    /**
     * Calcula los días con mayor volumen de tráfico total (Input + Output).
     * * Utiliza una PriorityQueue (Max-Heap) para ordenar por consumo de forma eficiente.
     * @param limit Número máximo de resultados (Top N).
     * @return Lista de los días más activos.
     */
    public List<TrafficDayStats> topDays(int limit) {
        PriorityQueue<TrafficDayStats> pq = new PriorityQueue<>(
                Comparator.comparingDouble(
                        (TrafficDayStats d) -> d.getTotalInput() + d.getTotalOutput()
                ).reversed()
        );

        pq.addAll(index.values());

        List<TrafficDayStats> result = new ArrayList<>();
        for (int i = 0; i < limit && !pq.isEmpty(); i++) {
            result.add(pq.poll());
        }
        return result;
    }
}