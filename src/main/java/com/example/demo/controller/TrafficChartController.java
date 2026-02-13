package com.example.demo.controller;

import com.example.demo.domain.TrafficDayStats;
import com.example.demo.service.CsvTrafficIndexService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Controlador TrafficChartController
 * * Provee los puntos de acceso de datos para la visualización de analíticas.
 * Actúa como la fuente de datos principal para gráficas de consumo, permitiendo
 * consultar estadísticas agregadas por días, rangos personalizados o récords de tráfico.
 */
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/traffic")
public class TrafficChartController {

    // --- DEPENDENCIAS ---

    /** Servicio especializado en la indexación y filtrado de archivos CSV de tráfico */
    private final CsvTrafficIndexService service;

    /**
     * Inyección del servicio de indexación.
     * @param service Servicio que procesa la lógica de agregación de datos.
     */
    public TrafficChartController(CsvTrafficIndexService service) {
        this.service = service;
    }

    // --- ENDPOINTS PARA GRÁFICAS (SERIES TEMPORALES) ---

    /**
     * Obtiene las estadísticas de los últimos 'n' días.
     * * Es el endpoint por defecto utilizado por el dashboard para la vista semanal.
     *
     * @param days Número de días hacia atrás a consultar.
     * @return Lista de {@link TrafficDayStats} con el consumo diario.
     */
    @GetMapping("/last/{days}")
    public List<TrafficDayStats> lastDays(@PathVariable int days) {
        return service.lastDays(days);
    }

    /**
     * Consulta el tráfico registrado entre dos fechas específicas.
     * * Utilizado por el selector de fechas personalizado del frontend.
     *
     * @param from Fecha de inicio del reporte.
     * @param to Fecha de fin del reporte.
     * @return Lista de estadísticas dentro del periodo solicitado.
     */
    @GetMapping("/range")
    public List<TrafficDayStats> range(@RequestParam LocalDate from, @RequestParam LocalDate to) {
        return service.between(from, to);
    }

    /**
     * Identifica los días con mayor volumen de tráfico (picos de consumo).
     *
     * @param limit Cantidad de registros máximos a retornar (por defecto 5).
     * @return Lista de los días con mayor tráfico detectado.
     */
    @GetMapping("/top")
    public List<TrafficDayStats> top(@RequestParam(defaultValue = "5") int limit) {
        return service.topDays(limit);
    }
}