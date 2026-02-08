package com.example.demo.controller;
import com.example.demo.web.dto.RegisterForm;
import com.example.demo.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/register")
public class RegisterController {

    private final UserService service;

    public RegisterController(UserService service) {
        this.service = service;
    }

    @GetMapping
    public String form(Model model) {

        model.addAttribute("form", new RegisterForm());
        return "register";
    }

    @PostMapping
    public String submit(@ModelAttribute("form") RegisterForm form,
                         Model model) {

        try {

            service.register(
                    form.getName(),
                    form.getEmail(),
                    form.getRole(),
                    form.getPassword()
            );

            model.addAttribute("ok", true);
            return "register";

        } catch (Exception e) {

            model.addAttribute("error", e.getMessage());
            return "register";
        }
    }
}
