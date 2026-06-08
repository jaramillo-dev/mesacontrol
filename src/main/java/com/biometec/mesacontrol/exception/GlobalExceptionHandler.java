package com.biometec.mesacontrol.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.Model;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

/**
 * Manejador global de excepciones para el controlador MVC con Thymeleaf.
 *
 * <p>Intercepta las excepciones lanzadas por cualquier controlador anotado
 * con {@code @Controller} y las transforma en respuestas HTML apropiadas,
 * redireccionando a vistas de error o añadiendo mensajes de retroalimentación
 * al flujo de navegación.</p>
 *
 * <p>Jerarquía de manejo:</p>
 * <ul>
 *   <li>{@link UsuarioNoEncontradoException} → vista {@code error/manejador-global}</li>
 *   <li>{@link EmailRegistradoException}     → vista {@code error/manejador-global}</li>
 *   <li>{@link IllegalArgumentException}     → vista {@code error/manejador-global}</li>
 *   <li>{@link Exception}                    → vista {@code error/manejador-global}</li>
 * </ul>
 *
 * @author Juan Jaramillo
 * @version 1.0
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    // ========================
    // 403 — ACCESO DENEGADO (SPRING SECURITY)
    // ========================

    /**
     * Captura las excepciones de Spring Security cuando un usuario autenticado
     * intenta acceder a un recurso para el cual no tiene el rol necesario
     * (ej. falla un @PreAuthorize).
     */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String manejarAccesoDenegado(AccessDeniedException ex,
                                        HttpServletRequest request,
                                        Model model) {
        model.addAttribute("codigo", 403);
        model.addAttribute("titulo", "Acceso denegado");
        model.addAttribute("mensaje", "No tienes los permisos necesarios para ver esta sección.");
        model.addAttribute("ruta", request.getRequestURI());

        return "error/manejador-global"; // Reutilizamos tu plantilla unificada
    }

    // ========================
    // 404 — RUTA NO ENCONTRADA (SPRING BOOT)
    // ========================

    /**
     * Captura los errores de enrutamiento cuando un usuario intenta acceder
     * a una URL que no existe en el sistema (ej. /usuario en lugar de /usuarios).
     */
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String manejarRutaNoEncontrada(NoResourceFoundException ex,
                                          HttpServletRequest request,
                                          Model model) {
        model.addAttribute("codigo", 404);
        model.addAttribute("titulo", "Página no encontrada");
        model.addAttribute("mensaje", "La dirección web a la que intentas acceder no existe o fue movida.");
        model.addAttribute("ruta", request.getRequestURI());

        // Retornamos a tu vista unificada. (En tu snippet pusiste "error/manejador-global",
        // pero puedes usar "error/404" si usaste la plantilla que te pasé antes).
        return "error/manejador-global";
    }

    // ========================
    // 404 — RECURSO NO ENCONTRADO
    // ========================

    /**
     * Maneja los casos en que un usuario no existe en la base de datos,
     * ya sea buscado por ID o por email.
     *
     * <p>Típicamente ocurre cuando alguien accede directamente a una URL
     * como {@code /usuarios/99/editar} con un ID inexistente.</p>
     *
     * @param ex      excepción capturada con el mensaje descriptivo
     * @param request petición HTTP para extraer la URL que causó el error
     * @param model   modelo de la vista de error
     * @return nombre de la plantilla Thymeleaf de error 404
     */
    @ExceptionHandler(UsuarioNoEncontradoException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String manejarUsuarioNoEncontrado(UsuarioNoEncontradoException ex,
                                             HttpServletRequest request,
                                             Model model) {
        model.addAttribute("codigo", 404);
        model.addAttribute("titulo", "Usuario no encontrado");
        model.addAttribute("mensaje", ex.getMessage());
        model.addAttribute("ruta", request.getRequestURI());
        return "error/manejador-global";
    }

    // ========================
    // 409 — CONFLICTO DE NEGOCIO
    // ========================

    /**
     * Maneja los intentos de registrar un email que ya existe en el sistema.
     *
     * <p>Aunque en el controlador MVC este caso ya se captura localmente con
     * {@code result.rejectValue()} para recargar el formulario, este handler
     * actúa como red de seguridad si la excepción escapa de ese bloque
     * (por ejemplo, desde un flujo no anticipado).</p>
     *
     * @param ex      excepción con el mensaje del email conflictivo
     * @param request petición HTTP para extraer la URL
     * @param model   modelo de la vista de error
     * @return nombre de la plantilla Thymeleaf de error 409
     */
    @ExceptionHandler(EmailRegistradoException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public String manejarEmailRegistrado(EmailRegistradoException ex,
                                         HttpServletRequest request,
                                         Model model) {
        model.addAttribute("codigo", 409);
        model.addAttribute("titulo", "Email ya registrado");
        model.addAttribute("mensaje", ex.getMessage());
        model.addAttribute("ruta", request.getRequestURI());
        return "error/manejador-global";
    }

    // ========================
    // 400 — ARGUMENTO INVÁLIDO
    // ========================

    /**
     * Maneja errores de validación de negocio simples, como la contraseña
     * obligatoria en creación, que se expresan como {@link IllegalArgumentException}.
     *
     * <p>No confundir con {@link MethodArgumentNotValidException}: esa la
     * genera Bean Validation sobre los DTOs antes de llegar al servicio.
     * Esta la lanza el servicio cuando una regla de negocio falla.</p>
     *
     * @param ex      excepción con la descripción del argumento inválido
     * @param request petición HTTP
     * @param model   modelo de la vista de error
     * @return nombre de la plantilla Thymeleaf de error 400
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String manejarArgumentoInvalido(IllegalArgumentException ex,
                                           HttpServletRequest request,
                                           Model model) {
        model.addAttribute("codigo", 400);
        model.addAttribute("titulo", "Solicitud incorrecta");
        model.addAttribute("mensaje", ex.getMessage());
        model.addAttribute("ruta", request.getRequestURI());
        return "error/manejador-global";
    }

    /**
     * Maneja los errores de Bean Validation cuando fallan las anotaciones
     * {@code @Valid} o {@code @Validated} sobre un {@code @RequestBody}.
     *
     * <p>En el flujo MVC con Thymeleaf este caso normalmente no llega aquí
     * porque el controlador usa {@code BindingResult} para capturarlo y
     * recargar el formulario. Este handler es una red de seguridad para
     * endpoints donde {@code BindingResult} no esté presente.</p>
     *
     * @param ex    excepción con la lista de campos que fallaron
     * @param model modelo de la vista de error
     * @return nombre de la plantilla Thymeleaf de error 400
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String manejarValidacionDTO(MethodArgumentNotValidException ex,
                                       Model model) {
        // Construye un resumen legible de todos los campos que fallaron
        String detalle = ex.getBindingResult().getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));

        model.addAttribute("codigo", 400);
        model.addAttribute("titulo", "Datos inválidos");
        model.addAttribute("mensaje", "Corregí los siguientes campos: " + detalle);
        return "error/manejador-global";
    }

    // ========================
    // 500 — ERROR INESPERADO
    // ========================

    /**
     * Captura cualquier excepción no manejada explícitamente por los handlers
     * anteriores, evitando que el stack trace llegue al usuario.
     *
     * <p>En producción este handler es crítico: protege información sensible
     * y garantiza que el usuario siempre reciba una respuesta HTML coherente.</p>
     *
     * @param ex      excepción inesperada
     * @param request petición HTTP
     * @param model   modelo de la vista de error
     * @return nombre de la plantilla Thymeleaf de error 500
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String manejarErrorGeneral(Exception ex,
                                      HttpServletRequest request,
                                      Model model) {
        model.addAttribute("codigo", 500);
        model.addAttribute("titulo", "Error inesperado");
        model.addAttribute("mensaje", "Ocurrió un error interno. Por favor intentá más tarde.");
        model.addAttribute("ruta", request.getRequestURI());

        // El stack trace solo se loguea en el servidor, nunca se expone al cliente
        // log.error("Error no manejado en {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        return "error/manejador-global";
    }
}
