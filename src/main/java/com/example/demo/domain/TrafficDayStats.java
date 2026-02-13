package com.example.demo.domain;

import java.time.LocalDate;

/**
 * Entidad de Dominio: TrafficDayStats
 * * Representa el resumen consolidado de tráfico de red para un día específico.
 * Esta clase agrupa los datos de múltiples capturas individuales para proporcionar
 * una vista de alto nivel, ideal para alimentar gráficas de barras y líneas en el dashboard.
 */
public class TrafficDayStats {

    // --- DIMENSIONES TEMPORALES ---

    /** Fecha a la que corresponden las métricas (Año-Mes-Día) */
    private LocalDate date;

    // --- MÉTRICAS AGREGADAS (GB/MB) ---

    /** Sumatoria total del tráfico de entrada (Download) detectado durante el día */
    private double totalInput;

    /** Sumatoria total del tráfico de salida (Upload) detectado durante el día */
    private double totalOutput;

    // --- METADATOS DE ACTIVIDAD ---

    /** Número total de archivos de captura (.pcap) que contribuyeron a este resumen */
    private int captures;

    // --- CONSTRUCTORES ---

    /**
     * Constructor para la creación de registros agregados.
     * @param date Día calendario.
     * @param totalInput Volumen de entrada.
     * @param totalOutput Volumen de salida.
     * @param captures Cantidad de sesiones registradas.
     */
    public TrafficDayStats(LocalDate date, double totalInput, double totalOutput, int captures) {
        this.date = date;
        this.totalInput = totalInput;
        this.totalOutput = totalOutput;
        this.captures = captures;
    }

    // --- GETTERS Y SETTERS ---

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public double getTotalInput() {
        return totalInput;
    }

    public void setTotalInput(double totalInput) {
        this.totalInput = totalInput;
    }

    public double getTotalOutput() {
        return totalOutput;
    }

    public void setTotalOutput(double totalOutput) {
        this.totalOutput = totalOutput;
    }

    public int getCaptures() {
        return captures;
    }

    public void setCaptures(int captures) {
        this.captures = captures;
    }
}