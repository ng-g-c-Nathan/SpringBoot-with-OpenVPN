package com.example.demo.controller;

import com.example.demo.domain.CommandResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

/**
 * Controlador CommandController
 * * Proporciona una interfaz de control para gestionar el ciclo de vida del servicio VPN.
 * Permite ejecutar operaciones de administración (start, stop, restart, status)
 * mediante comandos de sistema (systemctl) de forma segura y controlada.
 */
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/vpn")
public class CommandController {

    // --- CONFIGURACIÓN DE COMANDOS (WHITELIST) ---

    /** * Mapa estático que define los comandos permitidos.
     * Actúa como una capa de seguridad para evitar la ejecución de comandos arbitrarios (RCE).
     */
    private static final Map<String, String[]> VPN_COMMANDS = new HashMap<>();

    static {
        // Mapeo de acciones amigables a comandos reales del sistema operativo
        VPN_COMMANDS.put("start",   new String[]{"systemctl", "start", "openvpn@server"});
        VPN_COMMANDS.put("stop",    new String[]{"systemctl", "stop", "openvpn@server"});
        VPN_COMMANDS.put("restart", new String[]{"systemctl", "restart", "openvpn@server"});
        VPN_COMMANDS.put("status",  new String[]{"systemctl", "status", "openvpn@server"});
    }

    // --- ENDPOINTS DE CONTROL ---

    /**
     * Ejecuta una acción de gestión sobre el servicio OpenVPN.
     * * Valida si el comando solicitado existe en la lista blanca y procesa la
     * ejecución mediante un proceso hijo del sistema.
     *
     * @param command El identificador del comando (start, stop, etc.).
     * @return ResponseEntity con un objeto {@link CommandResponse} que detalla el éxito, mensaje y salida de consola.
     */
    @PostMapping("/execute")
    public ResponseEntity<CommandResponse> executeVpnCommand(@RequestParam String command) {

        // --- VALIDACIÓN DE SEGURIDAD ---

        if (!VPN_COMMANDS.containsKey(command)) {
            return ResponseEntity.status(403)
                    .body(new CommandResponse("error", "Comando VPN no permitido o inválido", ""));
        }

        // --- LÓGICA DE EJECUCIÓN DE PROCESOS ---

        try {
            // Crea e inicia el proceso hijo con el comando seleccionado
            ProcessBuilder pb = new ProcessBuilder(VPN_COMMANDS.get(command));
            Process process = pb.start();

            // Captura de la salida del comando para diagnóstico
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            // Bloquea hasta que el comando termine y captura el código de salida (0 = éxito)
            int exitCode = process.waitFor();

            String status = (exitCode == 0) ? "success" : "error";
            String message = (exitCode == 0)
                    ? "Comando '" + command + "' ejecutado correctamente."
                    : "Error al ejecutar el comando '" + command + "' en el sistema.";

            return ResponseEntity.ok(new CommandResponse(status, message, output.toString().trim()));

        } catch (Exception e) {
            // Manejo de interrupciones o fallos de ejecución a nivel de Kernel/OS
            return ResponseEntity.status(500)
                    .body(new CommandResponse("error", "Excepción al ejecutar comando: " + e.getMessage(), ""));
        }
    }
}