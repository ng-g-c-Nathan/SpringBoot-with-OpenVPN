package com.example.demo.controller;

import com.example.demo.service.UserService;
import com.example.demo.web.dto.LoginForm;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class LoginController {

    private final UserService service;

    public LoginController(UserService service) {
        this.service = service;
    }


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
                    .body(java.util.Map.of("error", "Login error"));
        }
    }


    @GetMapping("/login")
    public String loginPage(Model model) {

        model.addAttribute("loginForm", new LoginForm());
        return "login";
    }

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

            model.addAttribute("error", true);
            return "login-result";
        }
    }
}
