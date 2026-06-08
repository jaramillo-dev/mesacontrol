package com.biometec.mesacontrol.controller;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Controlador dedicado a servir las vistas de error estáticas.
 * Spring Security redirige aquí directamente, sin pasar por ControllerAdvice.
 */
@Controller
@RequestMapping("/error")
public class ErrorController {

    @GetMapping("/401")
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public String error401(Model model) {
        model.addAttribute("codigo", 401);
        model.addAttribute("titulo", "No autenticado");
        model.addAttribute("mensaje",
                "Debes iniciar sesión para acceder a este recurso.");
        return "error/manejador-global";
    }

    @GetMapping("/403")
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String error403(Model model) {
        model.addAttribute("codigo", 403);
        model.addAttribute("titulo", "Acceso denegado");
        model.addAttribute("mensaje",
                "No tienes los permisos necesarios para ver esta sección.");
        return "error/manejador-global";
    }
}