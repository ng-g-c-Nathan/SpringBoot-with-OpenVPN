package com.example.demo.controller;

import com.example.demo.domain.CommandResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/vpn")
public class CommandController {


    private static final Map<String, String[]> VPN_COMMANDS = new HashMap<>();

    static {
        VPN_COMMANDS.put("start", new String[]{"systemctl", "start", "openvpn@server"});
        VPN_COMMANDS.put("stop", new String[]{"systemctl", "stop", "openvpn@server"});
        VPN_COMMANDS.put("restart", new String[]{"systemctl", "restart", "openvpn@server"});
        VPN_COMMANDS.put("status", new String[]{"systemctl", "status", "openvpn@server"});
    }

    @PostMapping("/execute")
    public ResponseEntity<CommandResponse> executeVpnCommand(@RequestParam String command) {
        if (!VPN_COMMANDS.containsKey(command)) {
            return ResponseEntity.status(403)
                    .body(new CommandResponse("error", "Comando VPN no permitido", ""));
        }

        try {
            ProcessBuilder pb = new ProcessBuilder(VPN_COMMANDS.get(command));
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            int exitCode = process.waitFor();
            String status = (exitCode == 0) ? "success" : "error";
            String message = (exitCode == 0)
                    ? "Comando '" + command + "' ejecutado correctamente."
                    : "Error al ejecutar el comando '" + command + "'.";

            return ResponseEntity.ok(new CommandResponse(status, message, output.toString().trim()));

        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(new CommandResponse("error", "Excepción al ejecutar comando", e.getMessage()));
        }
    }
}
