package com.example.demo.controller;

import com.example.demo.domain.Client;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Controlador ClientController
 * * Se encarga de monitorear y listar los clientes conectados al servidor VPN.
 * Extrae información en tiempo real directamente del archivo de estado de OpenVPN,
 * transformando registros planos en objetos de dominio estructurados.
 */
@RestController
public class ClientController {

    // --- CONFIGURACIÓN DE RUTAS ---

    /** Ruta del archivo de estado generado por OpenVPN (estándar en sistemas Linux) */
    private static final String STATUS_FILE = "/etc/openvpn/openvpn-status.log";

    // --- ENDPOINTS DE MONITOREO ---

    /**
     * Obtiene la lista de clientes actualmente conectados a la VPN.
     * * Lee el archivo de logs, filtra las líneas con el prefijo 'CLIENT_LIST'
     * y mapea los campos de tráfico y conexión.
     * * @return ResponseEntity con la lista de objetos {@link Client} o mensaje de error si el log no es accesible.
     */
    @GetMapping("/api/clients")
    public ResponseEntity<?> listClients() {

        // --- VERIFICACIÓN DE ARCHIVO ---

        File file = new File(STATUS_FILE);
        if (!file.exists()) {
            return ResponseEntity
                    .status(404)
                    .body("Archivo openvpn-status.log no encontrado en la ruta especificada.");
        }

        // --- PROCESAMIENTO Y PARSING DE DATA ---

        List<Client> clients = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                // Filtramos únicamente las líneas que contienen datos de clientes activos
                if (line.startsWith("CLIENT_LIST")) {
                    String[] parts = line.split(",");

                    // Verificamos que la línea tenga las columnas mínimas requeridas por el estándar OpenVPN
                    if (parts.length >= 6) {
                        Client c = new Client(
                                parts[1],                 // Common Name (Usuario)
                                parts[2],                 // Real Address (IP origen)
                                "",                       // Virtual Address (Opcional)
                                Long.parseLong(parts[3]), // Bytes Received
                                Long.parseLong(parts[4]), // Bytes Sent
                                parts[5],                 // Connected Since (Timestamp)
                                "online"                  // Estado operativo
                        );
                        clients.add(c);
                    }
                }
            }
        } catch (Exception e) {
            // Error de lectura de entrada/salida o formato de número
            return ResponseEntity
                    .status(500)
                    .body("Error procesando el archivo de estado: " + e.getMessage());
        }

        // Retornamos la colección de clientes mapeados
        return ResponseEntity.ok(clients);
    }
}