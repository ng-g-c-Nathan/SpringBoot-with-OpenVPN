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
            ProcessBuilder pb = new ProcessBuilder("systemctl", "show", "openvpn@server", "--no-page");
            pb.redirectErrorStream(true);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            Map<String, String> serviceInfo = new HashMap<>();

            while ((line = reader.readLine()) != null) {
                if (line.contains("=")) {
                    String[] parts = line.split("=", 2);
                    serviceInfo.put(parts[0], parts[1]);
                }
            }

            int exitCode = process.waitFor();

            if (exitCode == 0) {
                response.put("timestamp", LocalDateTime.now().toString());
                response.put("active", serviceInfo.getOrDefault("ActiveState", "unknown"));
                response.put("since", serviceInfo.getOrDefault("ActiveEnterTimestamp", "unknown"));
                response.put("main_pid", serviceInfo.getOrDefault("MainPID", "0"));
                response.put("status_message", serviceInfo.getOrDefault("StatusText", ""));
                return ResponseEntity.ok(response);
            } else {
                response.put("error", "No se pudo obtener el estado del servicio");
                return ResponseEntity.status(500).body(response);
            }

        } catch (Exception e) {
            response.put("error", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}
