package com.biometec.mesacontrol.controller;

import com.biometec.mesacontrol.dto.DashboardStatsDTO;
import com.biometec.mesacontrol.dto.TicketResponseDTO;
import com.biometec.mesacontrol.entity.Usuario;
import com.biometec.mesacontrol.repository.UsuarioRepository;
import com.biometec.mesacontrol.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

/**
 * Controlador responsable del flujo de autenticación de usuarios.
 *
 * <p>
 * Gestiona la presentación del formulario de inicio de sesión y las
 * redirecciones iniciales de la aplicación.
 * </p>
 *
 * <p>
 * La validación de credenciales es realizada por Spring Security
 * mediante el endpoint POST /login configurado en la cadena de filtros,
 * por lo que este controlador únicamente expone las vistas necesarias
 * para la interacción del usuario.
 * </p>
 *
 * @author Juan Jaramillo
 * @version 1.0
 */
@Controller
@RequestMapping("/")
@RequiredArgsConstructor
public class AuthController {

    private final TicketService ticketService;
    private final UsuarioRepository usuarioRepository;

    /**
     * Captura la ruta raíz de la aplicación.
     *
     * <p>
     * Debido a que el sistema utiliza una página de autenticación dedicada,
     * cualquier acceso a la URL raíz se redirige al formulario de login.
     * </p>
     *
     * @return redirección al formulario de autenticación
     */
    @GetMapping
    public String raiz() {
        return "redirect:/login";
    }

    /**
     * Muestra el formulario de inicio de sesión.
     *
     * <p>
     * Esta vista es utilizada por Spring Security como punto de entrada
     * para la autenticación mediante formulario.
     * </p>
     *
     * <p>
     * El procesamiento de las credenciales no ocurre en este método; es
     * realizado automáticamente por Spring Security mediante el endpoint
     * POST /login.
     * </p>
     *
     * @return nombre de la vista Thymeleaf del formulario de login
     */
    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }

    /**
     * Muestra el panel principal de la aplicación para el usuario autenticado.
     *
     * <p>
     * Recupera las estadísticas consolidadas de tickets asociadas al usuario
     * actual y las expone a la vista para construir los indicadores del dashboard.
     * </p>
     *
     * <p>
     * Aunque la ruta se encuentra protegida por Spring Security, se realiza una
     * validación defensiva sobre el principal autenticado para evitar errores en
     * escenarios excepcionales donde la sesión no esté disponible.
     * </p>
     *
     * @param userDetails información del usuario autenticado inyectada por Spring Security
     * @param model modelo utilizado para transferir datos a la vista Thymeleaf
     * @return la vista principal del dashboard o una redirección al login si no existe sesión activa
     */
    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        if (userDetails == null) {
            return "redirect:/login";
        }
        // Recuperamos el DTO consolidado
        DashboardStatsDTO stats = ticketService.obtenerTicketsParaDashboard(userDetails.getUsername());

        model.addAttribute("username", userDetails.getUsername());
        model.addAttribute("roles", userDetails.getAuthorities());
        model.addAttribute("stats", stats); // Pasamos las estadísticas con un nombre semántico

        return "dashboard/dashboard";
    }
}