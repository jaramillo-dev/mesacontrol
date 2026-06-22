package com.biometec.mesacontrol.controller;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Controlador responsable de mostrar las vistas de error personalizadas
 * de la aplicación.
 *
 * <p>
 * Centraliza la presentación de errores relacionados con autenticación,
 * autorización y recursos inexistentes para evitar exponer páginas de
 * error genéricas de Spring Boot.
 * </p>
 *
 * <p>
 * Estas vistas permiten ofrecer una experiencia más amigable al usuario
 * final y mantener una apariencia consistente con el resto del sistema.
 * </p>
 *
 * <ul>
 *     <li>401 - Usuario no autenticado.</li>
 *     <li>403 - Usuario autenticado sin permisos suficientes.</li> *
 * </ul>
 *
 * @author Juan Jaramillo
 * @version 1.0
 */
@Controller
@RequestMapping("/error")
public class ErrorController {

    /**
     * Muestra la página de autenticación requerida.
     *
     * <p>
     * Actualmente la aplicación redirige a los usuarios no autenticados
     * al formulario de inicio de sesión. Esta vista se conserva para
     * posibles escenarios futuros donde se requiera mostrar explícitamente
     * un error 401.
     * </p>
     *
     * @return nombre de la vista Thymeleaf correspondiente al error 401
     */
    @GetMapping("/401")
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public String error401(Model model) {
        model.addAttribute("codigo", 401);
        model.addAttribute("titulo", "No autenticado");
        model.addAttribute("mensaje",
                "Debes iniciar sesión para acceder a este recurso.");
        return "error/manejador-global";
    }

    /**
     * Muestra la página de acceso denegado.
     *
     * <p>
     * Esta vista se utiliza cuando un usuario autenticado intenta acceder
     * a un recurso protegido para el cual no posee los permisos o roles
     * requeridos.
     * </p>
     *
     * @return nombre de la vista Thymeleaf correspondiente al error 403
     */
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