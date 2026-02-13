package com.example.demo.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Controlador DashboardController
 * * Proporciona métricas de salud y estado operativo del servicio VPN en tiempo real.
 * A diferencia de otros controladores, este consulta directamente los metadatos de Systemd
 * para obtener marcas de tiempo precisas y estados internos del proceso (PID).
 */
@CrossOrigin(origins = "*")
@RestController
public class DashboardController {

    // --- ENDPOINTS DE MONITOREO DE SISTEMA ---

    /**
     * Obtiene un resumen detallado del estado del servicio OpenVPN.
     * * Ejecuta 'systemctl show' para obtener una lista de propiedades clave-valor
     * que describen el ciclo de vida actual del demonio de red.
     * * @return ResponseEntity con un mapa conteniendo el estado (active), PID,
     * fecha de inicio (since) y mensajes de estado del sistema.
     */
    @GetMapping("/api/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboardStatus() {
        Map<String, Object> response = new HashMap<>();

        try {
            // --- EJECUCIÓN DE CONSULTA SYSTEMD ---

            /** * Usamos 'show' en lugar de 'status' porque entrega un formato
             * consistente de Key=Value, ideal para el procesamiento programático.
             */
            ProcessBuilder pb = new ProcessBuilder("systemctl", "show", "openvpn@server", "--no-page");
            pb.redirectErrorStream(true);
            Process process = pb.start();

            // --- PARSING DE PROPIEDADES DEL SERVICIO ---

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            Map<String, String> serviceInfo = new HashMap<>();

            while ((line = reader.readLine()) != null) {
                if (line.contains("=")) {
                    // Dividimos solo en el primer '=' para manejar valores que contengan el signo igual
                    String[] parts = line.split("=", 2);
                    serviceInfo.put(parts[0], parts[1]);
                }
            }

            int exitCode = process.waitFor();

            // --- CONSTRUCCIÓN DE LA RESPUESTA ---

            if (exitCode == 0) {
                // Metadatos para el frontend: estado visual, tiempo de uptime y PID de control
                response.put("timestamp", LocalDateTime.now().toString());
                response.put("active", serviceInfo.getOrDefault("ActiveState", "unknown"));
                response.put("since", serviceInfo.getOrDefault("ActiveEnterTimestamp", "unknown"));
                response.put("main_pid", serviceInfo.getOrDefault("MainPID", "0"));
                response.put("status_message", serviceInfo.getOrDefault("StatusText", ""));

                return ResponseEntity.ok(response);
            } else {
                response.put("error", "No se pudo obtener el estado del servicio mediante systemctl");
                return ResponseEntity.status(500).body(response);
            }

        } catch (Exception e) {
            // Manejo de errores de IO o interrupciones de proceso
            response.put("error", "Fallo crítico en el monitoreo: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}