package com.example.demo.domain;

/**
 * Entidad de Dominio: CommandResponse
 * * Estructura de respuesta estandarizada para la ejecución de comandos del sistema.
 * Se utiliza para encapsular el resultado de operaciones enviadas al Shell (como systemctl),
 * proporcionando un formato coherente que el frontend puede interpretar para mostrar
 * alertas de éxito o registros de consola.
 */
public class CommandResponse {

    // --- ATRIBUTOS DE RESPUESTA ---

    /** Estado categórico de la operación (ej: 'success', 'error', 'pending') */
    private String status;

    /** Mensaje descriptivo legible para el usuario final sobre el resultado de la acción */
    private String message;

    /** Salida cruda (stdout/stderr) capturada desde la terminal durante la ejecución */
    private String output;

    // --- CONSTRUCTORES ---

    /** Constructor vacío necesario para la deserialización de frameworks como Jackson */
    public CommandResponse() {
    }

    /**
     * Constructor principal para inicializar una respuesta completa tras un proceso.
     * @param status El estado final del comando.
     * @param message Breve explicación del resultado.
     * @param output Contenido textual retornado por el sistema operativo.
     */
    public CommandResponse(String status, String message, String output) {
        this.status = status;
        this.message = message;
        this.output = output;
    }

    // --- GETTERS Y SETTERS ---

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getOutput() {
        return output;
    }

    public void setOutput(String output) {
        this.output = output;
    }
}