package com.example.demo.controller;

import com.example.demo.service.UserService;
import com.example.demo.web.dto.LoginForm;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador LoginController
 * * Gestiona la autenticación de usuarios mediante un esquema dual.
 * Proporciona soporte para aplicaciones modernas (API REST con JSON) y
 * renderizado de vistas tradicionales para navegación web estándar.
 */
@Controller
public class LoginController {

    // --- DEPENDENCIAS ---

    /** Servicio de lógica de negocio para la gestión y validación de credenciales */
    private final UserService service;

    /**
     * Inyección de dependencias mediante constructor.
     * @param service Servicio de usuarios.
     */
    public LoginController(UserService service) {
        this.service = service;
    }

    // --- FLUJO API REST (JSON) ---

    /**
     * Endpoint de autenticación para clientes externos o SPAs.
     * * Valida las credenciales y retorna una respuesta puramente de datos.
     * * @param form Objeto DTO con email y contraseña recibido en el cuerpo de la petición.
     * @return ResponseEntity con un mapa booleano de éxito o mensaje de error.
     */
    @PostMapping("/api/login")
    @ResponseBody
    public ResponseEntity<?> loginApi(@RequestBody LoginForm form) {
        try {
            boolean ok = service.login(
                    form.getEmail(),
                    form.getPassword()
            );

            return ResponseEntity.ok(
                    java.util.Map.of("ok", ok)
            );

        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(java.util.Map.of("error", "Error interno en el proceso de login"));
        }
    }

    // --- FLUJO WEB (VISTAS HTML) ---

    /**
     * Renderiza la página de inicio de sesión.
     * * Prepara el modelo con un objeto de formulario vacío para el binding de datos.
     * * @param model Contenedor de atributos para la vista.
     * @return Nombre de la plantilla HTML (login).
     */
    @GetMapping("/login")
    public String loginPage(Model model) {
        model.addAttribute("loginForm", new LoginForm());
        return "login";
    }

    /**
     * Procesa la solicitud de login enviada desde el formulario web.
     * * A diferencia de la API, este método redirige el flujo a una vista de resultado.
     * * @param loginForm Datos capturados del formulario HTML.
     * @param model Atributos para la vista de respuesta.
     * @return Nombre de la plantilla de resultado (login-result).
     */
    @PostMapping("/login")
    public String loginView(
            @ModelAttribute LoginForm loginForm,
            Model model
    ) {
        try {
            boolean ok = service.login(
                    loginForm.getEmail(),
                    loginForm.getPassword()
            );

            model.addAttribute("ok", ok);
            return "login-result";

        } catch (Exception e) {
            // Se marca el error en el modelo para feedback visual en la UI
            model.addAttribute("error", true);
            return "login-result";
        }
    }
}