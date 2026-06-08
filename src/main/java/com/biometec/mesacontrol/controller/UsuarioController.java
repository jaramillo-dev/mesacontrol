package com.biometec.mesacontrol.controller;

import com.biometec.mesacontrol.dto.*;
import com.biometec.mesacontrol.mapper.UsuarioMapper;
import com.biometec.mesacontrol.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controlador MVC para la gestión de usuarios mediante plantillas Thymeleaf.
 *
 * <p>Este componente se encarga de procesar las peticiones HTTP de la interfaz web,
 * interactuar con el {@link UsuarioService} y renderizar las vistas HTML.
 * En lugar de retornar JSON, retorna el nombre de la plantilla Thymeleaf a
 * procesar e interactúa con el {@link Model} de Spring.</p>
 *
 * <p>Sigue la regla de no exponer entidades JPA, utilizando DTOs
 * para la captura y validación de datos en los formularios.</p>
 *
 * @author Juan Jaramillo
 * @version 1.0
 * @see UsuarioService
 */
@Controller
@RequestMapping("/usuarios")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final UsuarioMapper usuarioMapper;

    // ========================
    // LISTADO
    // ========================

    /**
     * Renderiza la vista principal con la tabla paginada de usuarios.
     *
     * @param pageable configuración de paginación inyectada por Spring
     * @param model    modelo para inyectar datos a la vista Thymeleaf
     * @return nombre de la plantilla Thymeleaf (templates/usuarios/listar.html)
     */
    @GetMapping
    public String listarTodos(
            @PageableDefault(size = 10, sort = "nombre") Pageable pageable,
            Model model) {

        UsuarioPageResponseDTO pagina = usuarioService.listarTodos(pageable);
        model.addAttribute("pagina", pagina);
        return "usuarios/listar";
    }

    // ========================
    // CREACIÓN
    // ========================

    /**
     * Muestra el formulario en blanco para crear un nuevo usuario.
     *
     * @param model modelo de la vista
     * @return nombre de la plantilla del formulario
     */
    @GetMapping("/nuevo")
    public String mostrarFormularioCrear(Model model) {
        model.addAttribute("usuarioRequest", new UsuarioRequestDTO());
        model.addAttribute("modoEdicion", false);
        return "usuarios/formulario-usuario";
    }

    /**
     * Procesa el envío del formulario de creación y aplica Bean Validation.
     *
     * @param dto                datos del formulario ligados al DTO
     * @param result             resultados de la validación (errores)
     * @param redirectAttributes contenedor para mensajes "flash" (temporales)
     * @param model              modelo de la vista en caso de recarga por error
     * @return redirección al listado o recarga del formulario si hay errores
     */
    @PostMapping("/nuevo")
    public String procesarCreacion(
            @Validated(OnCreate.class) @ModelAttribute("usuarioRequest") UsuarioRequestDTO dto,
            BindingResult result,
            RedirectAttributes redirectAttributes,
            Model model) {

        if (result.hasErrors()) {
            model.addAttribute("modoEdicion", false);
            return "usuarios/formulario-usuario";
        }

        try {
            usuarioService.crearUsuario(dto);
            redirectAttributes.addFlashAttribute("mensajeExito", "Usuario creado correctamente.");
            return "redirect:/usuarios";
        } catch (IllegalArgumentException e) {
            // Captura errores de negocio (ej. email duplicado)
            result.rejectValue("email", "error.usuario", e.getMessage());
            model.addAttribute("modoEdicion", false);
            return "usuarios/formulario-usuario";
        }
    }

    // ========================
    // EDICIÓN
    // ========================

    /**
     * Muestra el formulario pre-poblado para editar un usuario.
     *
     * @param id    identificador del usuario a editar
     * @param model modelo de la vista
     * @return nombre de la plantilla del formulario
     */
    @GetMapping("/{id}/editar")
    public String mostrarFormularioEditar(@PathVariable Long id, Model model) {
        UsuarioResponseDTO usuario = usuarioService.obtenerPorId(id);
        UsuarioRequestDTO dto = usuarioMapper.toRequestDTOFromResponse(usuario);

        model.addAttribute("usuarioRequest", dto);
        model.addAttribute("usuarioId", id);
        model.addAttribute("modoEdicion", true);

        return "usuarios/formulario-usuario";
    }

    /**
     * Procesa el envío del formulario de actualización.
     *
     * @param id                 identificador del usuario
     * @param dto                datos del formulario ligados al DTO
     * @param result             resultados de validación
     * @param redirectAttributes mensajes temporales
     * @param model              modelo de la vista en caso de recarga por error
     * @return redirección al listado o recarga del formulario
     */
    @PostMapping("/{id}/editar")
    public String procesarEdicion(
            @PathVariable Long id,
            @Validated(OnUpdate.class) @ModelAttribute("usuarioRequest") UsuarioRequestDTO dto,
            BindingResult result,
            RedirectAttributes redirectAttributes,
            Model model) {

        if (result.hasErrors()) {
            model.addAttribute("usuarioId", id);
            model.addAttribute("modoEdicion", true);
            return "usuarios/formulario-usuario";
        }

        try {
            usuarioService.editarUsuario(id, dto);
            redirectAttributes.addFlashAttribute("mensajeExito", "Usuario actualizado correctamente.");
            return "redirect:/usuarios";
        } catch (IllegalArgumentException e) {
            result.rejectValue("email", "error.usuario", e.getMessage());
            model.addAttribute("usuarioId", id);
            model.addAttribute("modoEdicion", true);
            return "usuarios/formulario-usuario";
        }
    }

    // ========================
    // ACTIVACIÓN / DESACTIVACIÓN
    // ========================

    /**
     * Alterna el estado activo/inactivo de un usuario.
     *
     * <p>Utilizamos POST para mayor compatibilidad de los botones en
     * las tablas de la interfaz Thymeleaf.</p>
     *
     * @param id                 identificador del usuario
     * @param redirectAttributes atributos para mostrar mensajes de retroalimentación
     * @return redirección al listado
     */
    @PostMapping("/{id}/toggle-estado")
    public String alternarEstado(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            UsuarioResponseDTO usuario = usuarioService.toggleActivacion(id);
            String estado = usuario.estaActivo() ? "activado" : "desactivado";
            redirectAttributes.addFlashAttribute("mensajeExito", "Usuario " + estado + " correctamente.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("mensajeError", e.getMessage());
        }
        return "redirect:/usuarios";
    }
}