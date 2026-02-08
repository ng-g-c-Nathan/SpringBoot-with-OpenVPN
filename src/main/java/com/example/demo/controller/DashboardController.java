package com.example.demo.controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
public class DashboardController {

    @GetMapping("/api/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboardStatus() {
        Map<String, Object> response = new HashMap<>();
        try {
            // Ejecutar comando de sistema
            ProcessBuilder pb = new ProcessBuilder("systemctl", "status", "openvpn@server");
            pb.redirectErrorStream(true);
            Process process = pb.start();

            // Leer la salida
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            int exitCode = process.waitFor();

            if (exitCode == 0) {
                response.put("timestamp", LocalDateTime.now().toString());
                response.put("openvpn_status", output.toString());
                return ResponseEntity.ok(response);
            } else {
                response.put("error", output.toString());
                return ResponseEntity.status(500).body(response);
            }

        } catch (Exception e) {
            response.put("error", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}
