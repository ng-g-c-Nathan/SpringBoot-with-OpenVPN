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

@RestController
public class ClientController {

    private static final String STATUS_FILE = "/etc/openvpn/openvpn-status.log";

    @GetMapping("/api/clients")
    public ResponseEntity<?> listClients() {
        File file = new File(STATUS_FILE);
        if (!file.exists()) {
            return ResponseEntity.status(404).body("Archivo openvpn-status.log no encontrado");
        }

        List<Client> clients = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("CLIENT_LIST")) {
                    String[] parts = line.split(",");
                    if (parts.length >= 6) {
                        Client c = new Client(
                                parts[1],             // name
                                parts[2],             // real IP
                                "",                   // public IP
                                Long.parseLong(parts[3]), // bytesReceived
                                Long.parseLong(parts[4]), // bytesSent
                                parts[5],             // connectedSince
                                "online"              // status por defecto
                        );
                        clients.add(c);
                    }
                }
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }

        return ResponseEntity.ok(clients);
    }
}
