package com.example.demo.controller;

import com.example.demo.domain.Client;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Controlador ClientController
 *
 * Monitorea y lista los clientes conectados al servidor VPN.
 * Lee en tiempo real el archivo de estado de OpenVPN.
 */
@RestController
@RequestMapping("/api/clients")
@CrossOrigin(origins = "*")
public class ClientController {

    @Value("${openvpn.status.file:/etc/openvpn/openvpn-status.log}")
    private String STATUS_FILE;

    /**
     * Lista los clientes actualmente conectados a la VPN.
     * Formato OpenVPN status v1:
     *   CLIENT_LIST,CommonName,RealAddress,VirtualAddress,BytesReceived,BytesSent,ConnectedSince,ConnectedSinceT,Username,ClientID,PeerID
     */
    @GetMapping
    public ResponseEntity<?> listClients() {

        File file = new File(STATUS_FILE);
        if (!file.exists()) {
            return ResponseEntity.status(404)
                    .body("openvpn-status.log no encontrado — ¿está OpenVPN corriendo?");
        }

        List<Client> clients = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.startsWith("CLIENT_LIST")) continue;

                String[] parts = line.split(",");
                if (parts.length < 6) continue;

                // Ignorar la línea de cabecera
                if (parts[1].equalsIgnoreCase("Common Name")) continue;

                clients.add(new Client(
                        parts[1],                  // Common Name
                        parts[2],                  // Real Address (IP:puerto)
                        parts.length > 3 ? parts[3] : "",  // Virtual Address
                        parseLong(parts, 4),       // Bytes Received
                        parseLong(parts, 5),       // Bytes Sent
                        parts.length > 6 ? parts[6] : "", // Connected Since
                        "online"
                ));
            }
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body("Error leyendo status file: " + e.getMessage());
        }

        return ResponseEntity.ok(clients);
    }

    private long parseLong(String[] parts, int index) {
        try {
            return Long.parseLong(parts[index].trim());
        } catch (Exception e) {
            return 0L;
        }
    }
}