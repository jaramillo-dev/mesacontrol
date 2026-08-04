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
import io.github.wimdeblauwe.htmx.spring.boot.mvc.HtmxRequest;
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
     * Retorna la vista completa o únicamente un fragmento (tablaTickets) si la
     * solicitud es disparada vía HTMX.
     */
    @GetMapping
    public String listarTickets(@RequestParam(value = "folio", required = false) String folio,
                                @RequestParam(value = "filtro", required = false) String filtro,
                                @RequestParam(value = "vencido", required = false, defaultValue = "false") boolean vencido,
                                Model model, Principal principal,
                                @PageableDefault(size = 10) Pageable pageable,
                                HtmxRequest htmxRequest) {

        Usuario usuarioLogueado = usuarioRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new IllegalStateException("Usuario no encontrado"));

        String folioParam = (folio != null && !folio.trim().isEmpty()) ? folio.trim() : null;
        String filtroParam = (filtro != null && !filtro.trim().isEmpty()) ? filtro.trim().toUpperCase() : null;

        Page<TicketResponseDTO> tickets = ticketService.listarTicketsCombinados(folioParam, filtroParam, vencido, usuarioLogueado, pageable);

        model.addAttribute("tickets", tickets);
        model.addAttribute("folioBusqueda", folioParam);
        model.addAttribute("filtroActivo", filtroParam);
        model.addAttribute("soloVencidos", vencido);

        // Resolución dinámica de la vista
        if (htmxRequest.isHtmxRequest()) {
            return "tickets/listar-ticket :: tablaTickets";
        }

        return "tickets/listar-ticket";
    }

    @GetMapping("/{folio}")
    public String verDetalleTicket(@PathVariable String folio, Model model) {
        TicketResponseDTO ticket = ticketService.obtenerPorFolio(folio);
        List<ComentarioDTO> comentarios = ticketService.obtenerComentariosPorTicket(ticket.getId());

        model.addAttribute("ticket", ticket);
        model.addAttribute("comentarios", comentarios);

        return "tickets/detalle-ticket";
    }

    // ==========================================
    // 2. CREACIÓN DE TICKET (SOLO ADMIN)
    // ==========================================

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/nuevo")
    public String mostrarFormularioCreacion(Model model) {
        model.addAttribute("ticketDTO", new TicketRequestDTO());
        cargarCatalogosAlModelo(model);
        return "tickets/formulario-ticket";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/nuevo")
    public String procesarCreacionTicket(@Validated(OnCreate.class) @ModelAttribute("ticketDTO") TicketRequestDTO requestDTO,
                                         BindingResult result,
                                         Model model,
                                         RedirectAttributes flash) {

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

    @PostMapping("/{id}/iniciar")
    public String iniciarAtencionTicket(@PathVariable Long id, Model model,
                                        RedirectAttributes flash, HtmxRequest htmxRequest) {
        try {
            ticketService.iniciarAtencion(id);

            if (htmxRequest.isHtmxRequest()) {
                model.addAttribute("success", "El ticket ahora está EN PROCESO.");
                return renderFragmentoDetalle(id, model);
            }

            TicketResponseDTO ticket = ticketService.obtenerPorId(id);
            flash.addFlashAttribute("success", "El ticket ahora está EN PROCESO.");
            return "redirect:/tickets/" + ticket.getFolio();
        } catch (Exception e) {
            if (htmxRequest.isHtmxRequest()) {
                model.addAttribute("error", e.getMessage());
                return renderFragmentoDetalle(id, model);
            }
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/tickets";
        }
    }

    @PostMapping("/{id}/cerrar")
    public String cerrarTicket(@PathVariable Long id,
                               @RequestParam(required = false) String folioDocumento,
                               Model model, RedirectAttributes flash, HtmxRequest htmxRequest) {
        try {
            if (folioDocumento == null || folioDocumento.trim().isEmpty()) {
                throw new IllegalArgumentException("Debe ingresar el folio de cotización u orden de servicio para cerrar el ticket.");
            }

            ticketService.cerrarTicket(id, folioDocumento);

            if (htmxRequest.isHtmxRequest()) {
                model.addAttribute("success", "Ticket cerrado correctamente.");
                return renderFragmentoDetalle(id, model);
            }

            flash.addFlashAttribute("success", "Ticket cerrado correctamente.");
        } catch (Exception e) {
            if (htmxRequest.isHtmxRequest()) {
                model.addAttribute("error", e.getMessage());
                return renderFragmentoDetalle(id, model);
            }
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/tickets";
    }

    // ==========================================
    // 4. API INTERNA PARA COMBOS ANIDADOS
    // ==========================================

    @GetMapping("/api/equipos")
    @ResponseBody
    public List<EquipoComboDTO> obtenerEquiposPorCliente(@RequestParam Long clienteId) {
        return equipoRepository.findByClienteId(clienteId).stream()
                .map(equipo -> new EquipoComboDTO(equipo.getId(), equipo.getNombreEquipo(), equipo.getNumeroSerie()))
                .collect(Collectors.toList());
    }

    @GetMapping("/api/responsables")
    @ResponseBody
    public List<UsuarioComboDTO> obtenerResponsablesPorTipo(@RequestParam TipoTicket tipo) {
        Rol rolRequerido = (tipo == TipoTicket.VENTA) ? Rol.VENTAS : Rol.TECNICO;
        return usuarioRepository.findByRolAndActivo(rolRequerido, true).stream()
                .map(u -> new UsuarioComboDTO(u.getId(), u.getNombre()))
                .collect(Collectors.toList());
    }

    // ==========================================
    // 5. GESTIÓN DE COMENTARIOS (HISTORIAL)
    // ==========================================

    @PostMapping("/{folio}/comentarios")
    public String agregarComentario(@PathVariable String folio,
                                    @RequestParam("textoComentario") String textoComentario,
                                    Principal principal, Model model,
                                    RedirectAttributes flash, HtmxRequest htmxRequest) {
        try {
            String emailLogueado = principal.getName();
            TicketResponseDTO ticket = ticketService.obtenerPorFolio(folio);
            ticketService.agregarComentario(ticket.getId(), emailLogueado, textoComentario);

            if (htmxRequest.isHtmxRequest()) {
                model.addAttribute("success", "Comentario agregado al historial.");
                return renderFragmentoDetalle(folio, model);
            }

            flash.addFlashAttribute("success", "Comentario agregado al historial.");
        } catch (Exception e) {
            if (htmxRequest.isHtmxRequest()) {
                model.addAttribute("error", "No se pudo agregar el comentario: " + e.getMessage());
                return renderFragmentoDetalle(folio, model);
            }
            flash.addFlashAttribute("error", "No se pudo agregar el comentario: " + e.getMessage());
        }

        return "redirect:/tickets/" + folio;
    }

    // ==========================================
    // MÉTODOS AUXILIARES
    // ==========================================

    private void cargarCatalogosAlModelo(Model model) {
        model.addAttribute("clientes", clienteRepository.findAll());
        model.addAttribute("tipos", TipoTicket.values());
        model.addAttribute("prioridades", PrioridadTicket.values());
    }

    /**
     * Recarga el ticket (por id) y su historial de comentarios en el modelo, y
     * devuelve el fragmento HTMX "detalleTicket" listo para ser retornado.
     * Usado por las transiciones de estado (iniciar/cerrar), donde solo se cuenta con el id.
     */
    private String renderFragmentoDetalle(Long ticketId, Model model) {
        TicketResponseDTO ticket = ticketService.obtenerPorId(ticketId);
        List<ComentarioDTO> comentarios = ticketService.obtenerComentariosPorTicket(ticket.getId());
        model.addAttribute("ticket", ticket);
        model.addAttribute("comentarios", comentarios);
        return "tickets/detalle-ticket :: detalleTicket";
    }

    /**
     * Variante por folio, usada al agregar comentarios (el path variable disponible es el folio).
     */
    private String renderFragmentoDetalle(String folio, Model model) {
        TicketResponseDTO ticket = ticketService.obtenerPorFolio(folio);
        List<ComentarioDTO> comentarios = ticketService.obtenerComentariosPorTicket(ticket.getId());
        model.addAttribute("ticket", ticket);
        model.addAttribute("comentarios", comentarios);
        return "tickets/detalle-ticket :: detalleTicket";
    }

    public record EquipoComboDTO(Long id, String nombre, String numeroSerie) {
        public String getDisplayName() {
            return nombre + " (S/N: " + numeroSerie + ")";
        }
    }

    public record UsuarioComboDTO(Long id, String nombre) {}
}