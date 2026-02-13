package com.example.demo.controller;

import com.example.demo.web.dto.RegisterForm;
import com.example.demo.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador RegisterController
 * * Se encarga de la gestión de altas de nuevos usuarios en la plataforma.
 * Proporciona la interfaz visual del formulario de registro y procesa la
 * persistencia de datos a través del servicio de usuarios.
 */
@Controller
@RequestMapping("/register")
public class RegisterController {

    // --- DEPENDENCIAS ---

    /** Servicio encargado de la lógica de negocio y validación de registros */
    private final UserService service;

    /**
     * Inyección de dependencias por constructor.
     * @param service Servicio de usuarios inyectado por Spring.
     */
    public RegisterController(UserService service) {
        this.service = service;
    }

    // --- GESTIÓN DEL FORMULARIO DE REGISTRO ---

    /**
     * Muestra la página con el formulario de registro.
     * * Inicializa un objeto DTO vacío para vincularlo con los campos del formulario HTML.
     *
     * @param model Contenedor de atributos para la vista.
     * @return Nombre de la plantilla de vista (register.html).
     */
    @GetMapping
    public String form(Model model) {
        // Vinculación del DTO con la vista para capturar datos
        model.addAttribute("form", new RegisterForm());
        return "register";
    }

    /**
     * Procesa la solicitud de creación de cuenta.
     * * Recibe los datos del formulario, invoca al servicio de registro y
     * retorna retroalimentación visual (éxito o error) a la misma vista.
     *
     * @param form Objeto {@link RegisterForm} con la información del nuevo usuario.
     * @param model Atributos de respuesta para mostrar mensajes en la UI.
     * @return Nombre de la vista para mostrar el resultado del proceso.
     */
    @PostMapping
    public String submit(@ModelAttribute("form") RegisterForm form,
                         Model model) {

        try {
            // --- LÓGICA DE PERSISTENCIA ---
            service.register(
                    form.getName(),
                    form.getEmail(),
                    form.getRole(),
                    form.getPassword()
            );

            // Flag de éxito para mostrar alerta de confirmación en el frontend
            model.addAttribute("ok", true);
            return "register";

        } catch (Exception e) {
            // Captura de errores (ej. email duplicado) para informar al usuario
            model.addAttribute("error", e.getMessage());
            return "register";
        }
    }
}