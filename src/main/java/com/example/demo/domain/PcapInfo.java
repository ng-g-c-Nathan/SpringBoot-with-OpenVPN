package com.example.demo.domain;

import java.time.Instant;

/**
 * Entidad de Dominio: PcapInfo
 * * Representa los metadatos de un archivo de captura de paquetes (.pcap) en el servidor.
 * Esta clase es fundamental para el flujo de trabajo de análisis, ya que rastrea
 * la existencia del archivo físico, su última modificación y si ya ha sido
 * transformado exitosamente a un reporte CSV.
 */
public class PcapInfo {

    // --- ATRIBUTOS DE ARCHIVO FÍSICO ---

    /** Nombre completo del archivo, incluyendo la extensión .pcap */
    private String name;

    /** Marca de tiempo de la última modificación en el sistema de archivos (UTC) */
    private Instant lastModified;

    /** Tamaño del archivo en bytes */
    private long size;

    // --- ATRIBUTOS DE PROCESAMIENTO ---

    /** * Estado actual del procesamiento del reporte vinculado.
     * Valores comunes: 'true' (procesado), 'false' (error/expirado), 'pending' (en espera).
     */
    private String csvStatus;

    /** Ruta relativa del archivo CSV generado a partir de este PCAP */
    private String csvPath;

    // --- CONSTRUCTORES ---

    /**
     * Constructor completo para representar el estado de una captura.
     * @param name Nombre del archivo.
     * @param lastModified Instante de modificación.
     * @param size Peso en bytes.
     * @param csvStatus Estado del proceso de conversión.
     * @param csvPath Referencia al archivo de salida.
     */
    public PcapInfo(String name, Instant lastModified, long size, String csvStatus, String csvPath) {
        this.name = name;
        this.lastModified = lastModified;
        this.size = size;
        this.csvStatus = csvStatus;
        this.csvPath = csvPath;
    }

    // --- GETTERS Y SETTERS ---

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Instant getLastModified() {
        return lastModified;
    }

    public void setLastModified(Instant lastModified) {
        this.lastModified = lastModified;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getCsvStatus() {
        return csvStatus;
    }

    public void setCsvStatus(String csvStatus) {
        this.csvStatus = csvStatus;
    }

    public String getCsvPath() {
        return csvPath;
    }

    public void setCsvPath(String csvPath) {
        this.csvPath = csvPath;
    }
}