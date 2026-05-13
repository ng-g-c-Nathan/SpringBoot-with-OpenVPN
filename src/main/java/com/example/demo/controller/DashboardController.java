package com.example.demo.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Controlador REST que expone el estado actual del servicio OpenVPN
 * para ser consumido por el dashboard del frontend.
 *
 * <p>Diseñado para entornos Docker donde <b>no existe</b> {@code systemd} ni
 * {@code systemctl}. La información se obtiene directamente de dos fuentes:</p>
 * <ol>
 *   <li><b>{@value #PID_FILE}</b> — determina si el proceso OpenVPN está vivo
 *       comprobando la existencia de {@code /proc/<pid>}.</li>
 *   <li><b>{@value #STATUS_FILE}</b> — archivo de log generado por OpenVPN con
 *       la opción {@code --status}; provee el timestamp de última actualización
 *       y la lista de clientes activos.</li>
 * </ol>
 *
 * <p>La estructura de la respuesta JSON es compatible con el frontend existente;
 * no se requieren cambios en el cliente.</p>
 *
 * @author  demo-team
 * @version 1.0
 * @see     com.example.demo.controller.CommandController
 */
@CrossOrigin(origins = "*")
@RestController
public class DashboardController {

    /**
     * Ruta al archivo de estado generado por OpenVPN cuando se arranca con
     * la opción {@code --status <ruta> <intervalo>}.
     * Contiene la lista de clientes conectados y un timestamp de última
     * actualización.
     */
    private static final String STATUS_FILE = "/etc/openvpn/openvpn-status.log";

    /**
     * Ruta al archivo donde OpenVPN escribe su PID de proceso al iniciarse
     * con la opción {@code --writepid} en modo {@code --daemon}.
     */
    private static final String PID_FILE = "/etc/openvpn/openvpn.pid";

    /**
     * Devuelve el estado actual del servicio OpenVPN en formato JSON.
     *
     * <p>El método realiza tres pasos en orden:</p>
     * <ol>
     *   <li><b>Detección de proceso</b> — lee {@value #PID_FILE} y comprueba
     *       si {@code /proc/<pid>} existe para determinar si OpenVPN está activo.</li>
     *   <li><b>Timestamp de estado</b> — parsea la línea {@code Updated,...} del
     *       archivo {@value #STATUS_FILE} para obtener la hora de última actualización.</li>
     *   <li><b>Construcción de respuesta</b> — ensambla el mapa de resultado con
     *       los campos esperados por el frontend.</li>
     * </ol>
     *
     * <p><b>Campos del JSON de respuesta:</b></p>
     * <ul>
     *   <li>{@code timestamp}      — Fecha y hora del servidor en el momento de la consulta
     *                                ({@link LocalDateTime#now()}).</li>
     *   <li>{@code active}         — {@code "active"} si el proceso existe en {@code /proc},
     *                                {@code "inactive"} en caso contrario.</li>
     *   <li>{@code since}          — Timestamp extraído del log de OpenVPN, o
     *                                {@code "unknown"} si no está disponible.</li>
     *   <li>{@code main_pid}       — PID del proceso OpenVPN, o {@code "0"} si no
     *                                se encontró el PID file.</li>
     *   <li>{@code status_message} — Mensaje descriptivo del estado de la lectura
     *                                del log (útil para depuración).</li>
     * </ul>
     *
     * @return {@link ResponseEntity} con {@code 200 OK} y el mapa de estado.
     *         Nunca devuelve {@code 4xx} ni {@code 5xx}; los errores de I/O se
     *         capturan internamente y se reflejan en {@code status_message}.
     */
    @GetMapping("/api/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboardStatus() {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now().toString());
        String mainPid = "0";
        String activeState = "inactive";
        try {
            Path pidPath = Path.of(PID_FILE);
            if (Files.exists(pidPath)) {
                mainPid = Files.readString(pidPath).trim();
                // Verificamos que el PID realmente existe en /proc (Linux)
                if (Files.exists(Path.of("/proc/" + mainPid))) {
                    activeState = "active";
                }
            }
        } catch (Exception ignored) {}

        String since = "unknown";
        String statusText = "OpenVPN corriendo en modo standalone";
        try {
            File statusFile = new File(STATUS_FILE);
            if (statusFile.exists()) {

                try (BufferedReader br = new BufferedReader(new FileReader(statusFile))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        if (line.startsWith("Updated,")) {
                            since = line.replace("Updated,", "").trim();
                            break;
                        }
                    }
                }
                statusText = "Leyendo estado desde openvpn-status.log";
            } else {
                statusText = "Log de estado no encontrado aún";
            }
        } catch (Exception e) {
            statusText = "Error leyendo log: " + e.getMessage();
        }

        response.put("active", activeState);
        response.put("since", since);
        response.put("main_pid", mainPid);
        response.put("status_message", statusText);

        return ResponseEntity.ok(response);
    }
}