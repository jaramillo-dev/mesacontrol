package com.biometec.mesacontrol.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controlador para las vistas principales y de autenticación.
 * Conecta las rutas de login y dashboard con sus respectivas plantillas Thymeleaf.
 */
@Controller
public class AuthController {

    /**
     * Captura la ruta raíz de la aplicación y redirige al login.
     */
    @GetMapping("/")
    public String raiz() {
        return "redirect:/login";
    }

    /**
     * Muestra la pantalla de login.
     * Spring Security interceptará automáticamente el POST a esta misma URL.
     */
    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }

    /**
     * Pantalla de bienvenida posterior al login exitoso.
     * Usamos @AuthenticationPrincipal para inyectar los datos del usuario logueado directamente en la vista.
     */
    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        if (userDetails != null) {
            model.addAttribute("username", userDetails.getUsername());
            model.addAttribute("roles", userDetails.getAuthorities());
        }
        return "dashboard/dashboard";
    }
}