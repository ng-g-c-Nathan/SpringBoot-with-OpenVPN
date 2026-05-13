package com.example.demo.controller;

import com.example.demo.domain.CommandResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import java.io.*;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Controlador REST para la gestión del ciclo de vida de OpenVPN en entornos Docker.
 *
 * <p>Dado que Docker no dispone de {@code systemd}/{@code systemctl}, este controlador
 * administra OpenVPN directamente mediante señales POSIX al proceso y relanzándolo
 * cuando es necesario.</p>
 *
 * <p>Expone un único endpoint principal en {@code /api/vpn/execute} que acepta los
 * comandos: {@code start}, {@code stop}, {@code restart} y {@code status}.</p>
 *
 * <p><b>Archivos relevantes en el sistema:</b></p>
 * <ul>
 *   <li>{@code /etc/openvpn/openvpn.pid}    — PID del proceso OpenVPN activo.</li>
 *   <li>{@code /etc/openvpn/server.conf}    — Archivo de configuración de OpenVPN.</li>
 *   <li>{@code /etc/openvpn/openvpn-status.log} — Log de estado generado cada 10 s.</li>
 * </ul>
 *
 * @author  demo-team
 * @version 1.0
 */
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/vpn")
public class CommandController {

    private static final String PID_FILE    = "/etc/openvpn/openvpn.pid";
    private static final String CONFIG_FILE = "/etc/openvpn/server.conf";
    private static final String STATUS_FILE = "/etc/openvpn/openvpn-status.log";


    /**
     * Lee el PID actual de OpenVPN desde el archivo {@value #PID_FILE}.
     *
     * @return el PID leído como {@code int}, o {@code -1} si el archivo no existe
     *         o no puede ser parseado.
     */
    private int readPid() {
        try {
            Path p = Path.of(PID_FILE);
            if (Files.exists(p)) {
                return Integer.parseInt(Files.readString(p).trim());
            }
        } catch (Exception ignored) {}
        return -1;
    }

    /**
     * Comprueba si un proceso con el PID dado sigue activo consultando {@code /proc}.
     *
     * @param pid el identificador de proceso a verificar.
     * @return {@code true} si {@code /proc/<pid>} existe; {@code false} en caso contrario
     *         o si {@code pid} es menor o igual a cero.
     */
    private boolean isAlive(int pid) {
        return pid > 0 && Files.exists(Path.of("/proc/" + pid));
    }

    /**
     * Envía una señal POSIX al proceso OpenVPN activo usando el comando
     * de sistema {@code kill}.
     *
     * <p>Ejemplos de señales útiles:</p>
     * <ul>
     *   <li>{@code TERM} — Cierre limpio del proceso.</li>
     *   <li>{@code HUP}  — Recarga de configuración sin desconectar clientes.</li>
     * </ul>
     *
     * @param signal nombre o número de la señal a enviar (e.g. {@code "TERM"}, {@code "HUP"}).
     * @return la salida estándar del comando {@code kill}, o un mensaje indicando
     *         que OpenVPN no está corriendo si el PID no existe.
     * @throws Exception si ocurre un error al lanzar o esperar el subproceso.
     */
    private String sendSignal(String signal) throws Exception {
        int pid = readPid();
        if (!isAlive(pid)) return "OpenVPN no está corriendo (PID " + pid + " no encontrado)";

        ProcessBuilder pb = new ProcessBuilder("kill", "-" + signal, String.valueOf(pid));
        pb.redirectErrorStream(true);
        Process p = pb.start();
        String out = new String(p.getInputStream().readAllBytes());
        p.waitFor();
        return out.isBlank() ? "Señal " + signal + " enviada a PID " + pid : out;
    }

    /**
     * Inicia OpenVPN como un proceso demonio independiente, equivalente a
     * {@code systemctl start openvpn}.
     *
     * <p>El proceso se lanza con las opciones:</p>
     * <ul>
     *   <li>{@code --config}   — Ruta al archivo de configuración ({@value #CONFIG_FILE}).</li>
     *   <li>{@code --status}   — Escribe el estado en {@value #STATUS_FILE} cada 10 segundos.</li>
     *   <li>{@code --writepid} — Guarda el PID del demonio en {@value #PID_FILE}.</li>
     *   <li>{@code --daemon}   — Ejecuta en segundo plano.</li>
     * </ul>
     *
     * <p>Tras lanzar el proceso se espera 1 segundo para que OpenVPN escriba su PID
     * antes de verificar si arrancó correctamente.</p>
     *
     * @return un mensaje indicando si OpenVPN se inició con éxito, si ya estaba
     *         corriendo, o si falló el arranque junto con la salida del proceso.
     * @throws Exception si ocurre un error al crear o esperar el subproceso.
     */
    private String startOpenvpn() throws Exception {
        if (isAlive(readPid())) return "OpenVPN ya está corriendo";

        ProcessBuilder pb = new ProcessBuilder(
                "openvpn",
                "--config", CONFIG_FILE,
                "--status", STATUS_FILE, "10",
                "--writepid", PID_FILE,
                "--daemon"
        );
        pb.redirectErrorStream(true);
        Process p = pb.start();
        String out = new String(p.getInputStream().readAllBytes());
        p.waitFor();

        Thread.sleep(1000); // Pequeña espera para que escriba el PID
        return isAlive(readPid()) ? "OpenVPN iniciado correctamente" : "Fallo al iniciar OpenVPN. " + out;
    }


    /**
     * Ejecuta una acción de gestión sobre el servicio OpenVPN.
     *
     * <p>Comandos soportados:</p>
     * <ul>
     *   <li>{@code start}   — Inicia OpenVPN si no está corriendo.</li>
     *   <li>{@code stop}    — Detiene OpenVPN enviando {@code SIGTERM} (cierre limpio).</li>
     *   <li>{@code restart} — Recarga la configuración enviando {@code SIGHUP}, sin
     *                         desconectar a los clientes activos.</li>
     *   <li>{@code status}  — Consulta si el proceso está activo y devuelve su PID.</li>
     * </ul>
     *
     * <p>Cualquier comando no reconocido devuelve un {@code 403 Forbidden}.</p>
     *
     * @param command el nombre del comando a ejecutar (case-sensitive).
     * @return un {@link ResponseEntity} con un {@link CommandResponse} que contiene:
     *         <ul>
     *           <li>{@code "success"} y la salida del comando si todo fue bien.</li>
     *           <li>{@code "error"} con descripción si el comando no está permitido
     *               o si se produce una excepción interna.</li>
     *         </ul>
     *         Códigos HTTP posibles: {@code 200 OK}, {@code 403 Forbidden},
     *         {@code 500 Internal Server Error}.
     */
    @PostMapping("/execute")
    public ResponseEntity<CommandResponse> executeVpnCommand(@RequestParam String command) {

        try {
            String output;
            String message;

            switch (command) {
                case "start" -> {
                    output  = startOpenvpn();
                    message = "Comando 'start' ejecutado.";
                }
                case "stop" -> {
                    // SIGTERM: cierre limpio de OpenVPN
                    output  = sendSignal("TERM");
                    message = "Comando 'stop' ejecutado.";
                }
                case "restart" -> {
                    // SIGHUP: OpenVPN recarga config sin desconectar clientes
                    output  = sendSignal("HUP");
                    message = "Comando 'restart' ejecutado (SIGHUP — recarga de config).";
                }
                case "status" -> {
                    int pid = readPid();
                    boolean alive = isAlive(pid);
                    output  = alive
                            ? "OpenVPN activo. PID: " + pid
                            : "OpenVPN NO está corriendo.";
                    message = "Estado consultado.";
                }
                default -> {
                    return ResponseEntity.status(403)
                            .body(new CommandResponse("error", "Comando VPN no permitido: " + command, ""));
                }
            }

            return ResponseEntity.ok(new CommandResponse("success", message, output));

        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(new CommandResponse("error", "Error ejecutando comando: " + e.getMessage(), ""));
        }
    }
}