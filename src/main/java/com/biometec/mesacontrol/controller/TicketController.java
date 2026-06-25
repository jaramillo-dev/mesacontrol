package com.biometec.mesacontrol.controller;

import com.biometec.mesacontrol.dto.ComentarioDTO;
import com.biometec.mesacontrol.dto.OnCreate;
import com.biometec.mesacontrol.dto.TicketRequestDTO;
import com.biometec.mesacontrol.dto.TicketResponseDTO;
import com.biometec.mesacontrol.entity.PrioridadTicket;
import com.biometec.mesacontrol.entity.Rol;
import com.biometec.mesacontrol.entity.TipoTicket;
import com.biometec.mesacontrol.entity.Usuario;
import com.biometec.mesacontrol.repository.ClienteRepository;
import com.biometec.mesacontrol.repository.EquipoRepository;
import com.biometec.mesacontrol.repository.UsuarioRepository;
import com.biometec.mesacontrol.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Controlador MVC responsable de la gestión operativa de tickets.
 *
 * <p>
 * Centraliza las operaciones relacionadas con consulta, creación,
 * seguimiento y actualización de tickets dentro de la mesa de servicio.
 * </p>
 *
 * <p>
 * Además de las vistas principales del módulo, expone endpoints internos
 * consumidos mediante AJAX para la carga dinámica de información en los
 * formularios de captura.
 * </p>
 *
 * <p>
 * La autorización de las operaciones se realiza mediante Spring Security
 * y anotaciones {@code @PreAuthorize}, permitiendo restringir determinadas
 * acciones según el rol del usuario autenticado.
 * </p>
 *
 * @author Juan Jaramillo
 * @version 1.0
 */
@Controller
@RequestMapping("/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;
    private final ClienteRepository clienteRepository;
    private final EquipoRepository equipoRepository;
    private final UsuarioRepository usuarioRepository;

    // ==========================================
    // 1. LISTADO Y DETALLE (ACCESO GENERAL)
    // ==========================================

    /**
     * Muestra el listado paginado de tickets visibles para el usuario autenticado.
     *
     * <p>
     * Permite realizar búsquedas por folio y filtrar por estado utilizando los
     * parámetros enviados desde la interfaz. La información devuelta depende del
     * rol y permisos del usuario conectado.
     * </p>
     *
     * <p>
     * Los parámetros de búsqueda son normalizados antes de enviarse a la capa de
     * servicio para evitar diferencias entre cadenas vacías y valores nulos.
     * </p>
     *
     * @param folio folio del ticket a buscar; opcional
     * @param filtro estado utilizado para filtrar los resultados; opcional
     * @param model modelo utilizado para exponer los resultados a la vista
     * @param principal usuario autenticado que realiza la consulta
     * @param vencido identifica si la fecha de atención venció
     * @param pageable configuración de paginación y ordenamiento
     * @return la vista de listado de tickets
     */
    @GetMapping
    public String listarTickets(@RequestParam(value = "folio", required = false) String folio,
                                @RequestParam(value = "filtro", required = false) String filtro,
                                @RequestParam(value = "vencido", required = false, defaultValue = "false") boolean vencido,
                                Model model, Principal principal,
                                @PageableDefault(size = 10) Pageable pageable) {

        Usuario usuarioLogueado = usuarioRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new IllegalStateException("Usuario no encontrado"));

        String folioParam = (folio != null && !folio.trim().isEmpty()) ? folio.trim() : null;
        String filtroParam = (filtro != null && !filtro.trim().isEmpty()) ? filtro.trim().toUpperCase() : null;

        // Llamamos al motor de búsqueda combinada enviando el flag de vencimiento
        Page<TicketResponseDTO> tickets = ticketService.listarTicketsCombinados(folioParam, filtroParam, vencido, usuarioLogueado, pageable);

        model.addAttribute("tickets", tickets);
        model.addAttribute("folioBusqueda", folioParam);
        model.addAttribute("filtroActivo", filtroParam);
        model.addAttribute("soloVencidos", vencido);

        return "tickets/listar-ticket";
    }

    /**
     * Muestra el detalle completo de un ticket específico.
     *
     * <p>
     * Recupera la información principal del ticket junto con el historial de
     * comentarios registrados durante su ciclo de vida para presentarlos en una
     * única vista.
     * </p>
     *
     * <p>
     * La cabecera del ticket y sus comentarios se obtienen de forma independiente
     * para mantener separadas las responsabilidades de consulta y facilitar la
     * evolución futura del módulo.
     * </p>
     *
     * @param folio identificador único visible para el usuario
     * @param model modelo utilizado para exponer la información del ticket y sus comentarios
     * @return la vista de detalle del ticket
     */
    @GetMapping("/{folio}")
    public String verDetalleTicket(@PathVariable String folio, Model model) {
        // 1. Obtenemos la cabecera (el ticket)
        TicketResponseDTO ticket = ticketService.obtenerPorFolio(folio);

        // 2. Obtenemos el detalle (el historial de comentarios) usando el ID del DTO recuperado
        List<ComentarioDTO> comentarios = ticketService.obtenerComentariosPorTicket(ticket.getId());

        // 3. Inyectamos ambos al modelo para que la vista los renderice de forma independiente
        model.addAttribute("ticket", ticket);
        model.addAttribute("comentarios", comentarios);

        return "tickets/detalle-ticket";
    }

    // ==========================================
    // 2. CREACIÓN DE TICKET (SOLO ADMIN)
    // ==========================================

    /**
     * Muestra el formulario para registrar un nuevo ticket.
     *
     * <p>
     * Inicializa un DTO vacío y carga los catálogos necesarios para completar
     * la captura, tales como clientes, equipos, técnicos y demás información
     * requerida por la vista.
     * </p>
     *
     * <p>
     * El acceso está restringido a usuarios con rol ADMIN debido a que la
     * creación de tickets forma parte del proceso de asignación y control
     * operativo del sistema.
     * </p>
     *
     * @param model modelo utilizado para exponer el DTO y los catálogos a la vista
     * @return la vista del formulario de creación de tickets
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/nuevo")
    public String mostrarFormularioCreacion(Model model) {
        model.addAttribute("ticketDTO", new TicketRequestDTO());
        cargarCatalogosAlModelo(model);
        return "tickets/formulario-ticket";
    }

    /**
     * Procesa la solicitud de creación de un nuevo ticket.
     *
     * <p>
     * Valida la información capturada utilizando las reglas definidas para la
     * operación de alta ({@link OnCreate}). Si existen errores de validación,
     * la vista es renderizada nuevamente conservando la información capturada
     * y los mensajes correspondientes.
     * </p>
     *
     * <p>
     * Cuando la validación es exitosa, delega la creación del ticket a la capa
     * de servicio y redirige al listado principal mostrando un mensaje de
     * confirmación.
     * </p>
     *
     * @param requestDTO información capturada para el nuevo ticket
     * @param result resultado de las validaciones ejecutadas sobre el formulario
     * @param model modelo utilizado para reenviar datos a la vista en caso de error
     * @param flash atributos temporales utilizados para mostrar mensajes después de la redirección
     * @return la vista del formulario si existen errores o una redirección al listado de tickets
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/nuevo")
    public String procesarCreacionTicket(@Validated(OnCreate.class) @ModelAttribute("ticketDTO") TicketRequestDTO requestDTO,
                                         BindingResult result,
                                         Model model,
                                         RedirectAttributes flash) {

        // Si la validación (Bean Validation) falla, recargamos la página con los errores
        if (result.hasErrors()) {
            cargarCatalogosAlModelo(model);
            return "tickets/formulario-ticket";
        }

        try {
            ticketService.crearTicket(requestDTO);
            flash.addFlashAttribute("success", "El ticket ha sido creado y asignado con éxito.");
            return "redirect:/tickets";
        } catch (Exception e) {
            model.addAttribute("error", "Error al crear el ticket: " + e.getMessage());
            cargarCatalogosAlModelo(model);
            return "tickets/formulario-ticket";
        }
    }

    // ==========================================
    // 3. TRANSICIONES DE ESTADO
    // ==========================================

    /**
     * Inicia la atención operativa de un ticket.
     *
     * <p>
     * Ejecuta la transición de estado correspondiente desde ABIERTO hacia
     * EN_PROCESO, siempre que se cumplan las reglas de negocio definidas por
     * la capa de servicio.
     * </p>
     *
     * <p>
     * Tras completar la operación, el usuario permanece en la pantalla de
     * detalle del ticket para continuar con su seguimiento.
     * </p>
     *
     * @param id identificador interno del ticket
     * @param flash atributos temporales utilizados para mostrar mensajes de éxito o error
     * @return redirección al detalle del ticket actualizado o al listado general en caso de error
     */
    @PostMapping("/{id}/iniciar")
    public String iniciarAtencionTicket(@PathVariable Long id, RedirectAttributes flash) {
        try {
            // 1. Ejecutamos el cambio de estado en la base de datos (ABIERTO -> EN_PROCESO)
            ticketService.iniciarAtencion(id);

            // 2. Recuperamos el ticket actualizado para conocer su folio único
            TicketResponseDTO ticket = ticketService.obtenerPorId(id);

            flash.addFlashAttribute("success", "El ticket ahora está EN PROCESO.");

            // 3. ¡REDIRECCIÓN INTERACTIVA!: Nos quedamos en la misma pantalla de detalle
            return "redirect:/tickets/" + ticket.getFolio();

        } catch (Exception e) {
            flash.addFlashAttribute("error", e.getMessage());
            // En caso de un error crítico, regresamos a la bandeja general por seguridad
            return "redirect:/tickets";
        }
    }

    /**
     * Cierra un ticket registrando el folio documental asociado a su resolución.
     *
     * <p>
     * Como parte del proceso de cierre, el sistema exige capturar un folio de
     * referencia que permita relacionar el ticket con la documentación generada
     * durante la atención, como una cotización, orden de servicio o documento
     * equivalente.
     * </p>
     *
     * <p>
     * La validación de este dato se realiza antes de delegar la operación a la
     * capa de servicio para evitar cierres incompletos.
     * </p>
     *
     * @param id identificador interno del ticket
     * @param folioDocumento folio documental asociado al cierre del ticket
     * @param flash atributos temporales utilizados para mostrar mensajes de éxito o error
     * @return redirección al listado principal de tickets
     */
    @PostMapping("/{id}/cerrar")
    public String cerrarTicket(@PathVariable Long id,
                               @RequestParam(required = false) String folioDocumento,
                               RedirectAttributes flash) {
        try {
            // Validamos a nivel controlador que no manden el folio vacío al cerrar
            if (folioDocumento == null || folioDocumento.trim().isEmpty()) {
                throw new IllegalArgumentException("Debe ingresar el folio de cotización u orden de servicio para cerrar el ticket.");
            }

            ticketService.cerrarTicket(id, folioDocumento);
            flash.addFlashAttribute("success", "Ticket cerrado correctamente.");
        } catch (Exception e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/tickets";
    }

    // ==========================================
    // 4. API INTERNA PARA COMBOS ANIDADOS
    // ==========================================

    /**
     * Obtiene los equipos médicos asociados a un cliente específico.
     *
     * <p>
     * Este endpoint es consumido mediante peticiones asíncronas (AJAX/Fetch API)
     * desde el formulario de creación de tickets para implementar la carga
     * dinámica de combos dependientes.
     * </p>
     *
     * <p>
     * La respuesta se proyecta a un DTO ligero para evitar exponer entidades JPA
     * completas y prevenir problemas de serialización relacionados con relaciones
     * perezosas (LazyInitializationException).
     * </p>
     *
     * @param clienteId identificador del cliente seleccionado en el formulario
     * @return listado de equipos asociados al cliente
     */
    @GetMapping("/api/equipos")
    @ResponseBody
    public List<EquipoComboDTO> obtenerEquiposPorCliente(@RequestParam Long clienteId) {
        // Mapeamos a un record simple para evitar el LazyInitializationException
        // de Jackson al intentar serializar las entidades JPA completas
        return equipoRepository.findByClienteId(clienteId).stream()
                .map(equipo -> new EquipoComboDTO(equipo.getId(), equipo.getNombreEquipo(), equipo.getNumeroSerie()))
                .collect(Collectors.toList());
    }

    /**
     * Obtiene los usuarios responsables disponibles para un tipo de ticket.
     *
     * <p>
     * Este endpoint es utilizado por la interfaz de creación para cargar
     * dinámicamente los responsables válidos según la naturaleza del ticket.
     * </p>
     *
     * <p>
     * La selección se realiza aplicando las reglas de negocio que relacionan
     * cada tipo de ticket con el área operativa correspondiente:
     * </p>
     *
     * <ul>
     *     <li>VENTA → usuarios con rol VENTAS</li>
     *     <li>SERVICIO → usuarios con rol TECNICO</li>
     * </ul>
     *
     * <p>
     * Únicamente se consideran usuarios activos para evitar asignaciones a
     * personal dado de baja.
     * </p>
     *
     * @param tipo tipo de ticket seleccionado por el usuario
     * @return listado de responsables disponibles para la operación solicitada
     */
    @GetMapping("/api/responsables")
    @ResponseBody
    public List<UsuarioComboDTO> obtenerResponsablesPorTipo(@RequestParam TipoTicket tipo) {
        // Mapeo de reglas de negocio: Relacionamos el Enum del Ticket con el Enum de Seguridad
        Rol rolRequerido = (tipo == TipoTicket.VENTA) ? Rol.VENTAS : Rol.TECNICO;

        // Retornamos solo usuarios de esa área que estén activos
        return usuarioRepository.findByRolAndActivo(rolRequerido, true).stream()
                .map(u -> new UsuarioComboDTO(u.getId(), u.getNombre()))
                .collect(Collectors.toList());
    }

    // ==========================================
    // 5. GESTIÓN DE COMENTARIOS (HISTORIAL)
    // ==========================================

    /**
     * Agrega un nuevo comentario al historial de seguimiento de un ticket.
     *
     * <p>
     * Los comentarios forman parte de la bitácora operativa del ticket y permiten
     * registrar avances, observaciones y acciones realizadas durante su ciclo
     * de atención.
     * </p>
     *
     * <p>
     * El comentario se asocia automáticamente al usuario autenticado que ejecuta
     * la acción para mantener la trazabilidad del historial.
     * </p>
     *
     * @param folio identificador funcional del ticket
     * @param textoComentario contenido del comentario capturado por el usuario
     * @param principal usuario autenticado que registra la observación
     * @param flash atributos temporales utilizados para mostrar mensajes después
     *              de la redirección
     * @return redirección a la vista de detalle del ticket
     */
    @PostMapping("/{folio}/comentarios")
    public String agregarComentario(@PathVariable String folio,
                                    @RequestParam("textoComentario") String textoComentario,
                                    Principal principal,
                                    RedirectAttributes flash) {
        try {
            String emailLogueado = principal.getName();

            // Primero obtenemos el ID del ticket usando su folio
            TicketResponseDTO ticket = ticketService.obtenerPorFolio(folio);

            // Guardamos el comentario usando el ID real
            ticketService.agregarComentario(ticket.getId(), emailLogueado, textoComentario);

            flash.addFlashAttribute("success", "Comentario agregado al historial.");
        } catch (Exception e) {
            flash.addFlashAttribute("error", "No se pudo agregar el comentario: " + e.getMessage());
        }

        // Redirigimos correctamente a la vista del folio
        return "redirect:/tickets/" + folio;
    }

    // ==========================================
    // MÉTODOS AUXILIARES
    // ==========================================

    /**
     * Carga en el modelo los catálogos requeridos por los formularios de tickets.
     *
     * <p>
     * Centraliza la preparación de datos utilizados por las vistas de creación
     * y edición para evitar duplicación de código y mantener consistencia en
     * los valores disponibles para el usuario.
     * </p>
     *
     * <p>
     * Entre los datos cargados se incluyen:
     * </p>
     *
     * <ul>
     *     <li>Clientes registrados.</li>
     *     <li>Tipos de ticket.</li>
     *     <li>Prioridades disponibles.</li>
     *     <li>Otros catálogos necesarios para la captura.</li>
     * </ul>
     *
     * @param model modelo al que se agregarán los catálogos requeridos por la vista
     */
    private void cargarCatalogosAlModelo(Model model) {
        // Carga la información que necesitan los <select> del formulario Thymeleaf
        model.addAttribute("clientes", clienteRepository.findAll());
        model.addAttribute("tipos", TipoTicket.values());
        model.addAttribute("prioridades", PrioridadTicket.values());

        // Idealmente solo traemos usuarios activos para que el ADMIN no asigne a alguien dado de baja
    }

    /**
     * DTO de proyección utilizado para poblar el selector de equipos médicos.
     *
     * <p>
     * Se utiliza exclusivamente como respuesta de los endpoints AJAX del
     * controlador para transportar únicamente la información necesaria para
     * construir los elementos del formulario.
     * </p>
     *
     * @param id identificador único del equipo
     * @param nombre nombre descriptivo del equipo
     * @param numeroSerie número de serie registrado
     */
    public record EquipoComboDTO(Long id, String nombre, String numeroSerie) {
        /**
         * Construye la representación textual mostrada al usuario dentro de los
         * elementos {@code <option>} del selector de equipos.
         *
         * @return nombre del equipo acompañado de su número de serie
         */
        public String getDisplayName() {
            return nombre + " (S/N: " + numeroSerie + ")";
        }
    }

    /**
     * DTO de proyección utilizado para poblar el selector de responsables.
     *
     * <p>
     * Expone únicamente la información mínima necesaria para la construcción
     * de listas desplegables en la interfaz de usuario.
     * </p>
     *
     * @param id identificador único del usuario
     * @param nombre nombre visible del responsable
     */
    public record UsuarioComboDTO(Long id, String nombre) {}
}