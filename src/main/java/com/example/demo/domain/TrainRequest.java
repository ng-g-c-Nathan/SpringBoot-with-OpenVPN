package com.example.demo.domain;

/**
 * Entidad de Dominio: TrainRequest
 * * Define los parámetros necesarios para iniciar un ciclo de re-entrenamiento del modelo.
 * Actúa como un objeto de transferencia de datos (DTO) que transporta la configuración
 * seleccionada por el usuario desde la interfaz de análisis hacia los scripts de Machine Learning.
 */
public class TrainRequest {

    // --- PARÁMETROS DE CONFIGURACIÓN ---

    /** * El modo de entrenamiento seleccionado.
     * Define el algoritmo o la estrategia específica a seguir
     */
    private String mode;

    /** * Fecha de inicio del conjunto de datos de entrenamiento.
     * Representada en formato ISO (YYYY-MM-DD).
     */
    private String fromDate;

    /** * Fecha de fin del conjunto de datos de entrenamiento.
     * Delimita el histórico de tráfico que será procesado por el modelo.
     */
    private String toDate;

    // --- GETTERS Y SETTERS ---

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getFromDate() {
        return fromDate;
    }

    public void setFromDate(String fromDate) {
        this.fromDate = fromDate;
    }

    public String getToDate() {
        return toDate;
    }

    public void setToDate(String toDate) {
        this.toDate = toDate;
    }
}