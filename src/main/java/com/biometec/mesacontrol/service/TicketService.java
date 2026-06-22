package com.biometec.mesacontrol.service;

import com.biometec.mesacontrol.dto.ComentarioDTO;
import com.biometec.mesacontrol.dto.DashboardStatsDTO;
import com.biometec.mesacontrol.dto.TicketRequestDTO;
import com.biometec.mesacontrol.dto.TicketResponseDTO;
import com.biometec.mesacontrol.entity.*;
import com.biometec.mesacontrol.exception.UsuarioNoEncontradoException;
import com.biometec.mesacontrol.mapper.TicketMapper;
import com.biometec.mesacontrol.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Servicio de negocio central para la gestión del ciclo de vida de los Tickets.
 * <p>
 * Se encarga de coordinar las reglas operativas de la Mesa de Control, incluyendo:
 * <ul>
 * <li>La validación de existencia de entidades satélite (Clientes, Equipos, Usuarios).</li>
 * <li>La autogeneración de folios institucionales bajo el formato TK-timestamp.</li>
 * <li>La aplicación de políticas de visibilidad según el rol del usuario autenticado.</li>
 * <li>La inmutabilidad de estados iniciales y obligatoriedad de asignación.</li>
 * </ul>
 * </p>
 *
 * @author Juan Jaramillo
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;
    private final ClienteRepository clienteRepository;
    private final EquipoRepository equipoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ComentarioRepository comentarioRepository;
    private final TicketMapper ticketMapper;

    /**
     * Orquesta la creación física de un nuevo Ticket en el sistema.
     * <p>
     * Aplica programación defensiva validando la integridad referencial de los catálogos.
     * Utiliza el modelo de dominio rico de la entidad Ticket para instanciación y
     * transiciones de estado (asignación de responsable).
     * </p>
     *
     * @param request DTO con la información de captura validada desde la UI
     * @return DTO de respuesta con los datos persistidos y calculados
     * @throws IllegalArgumentException si el cliente o el equipo médico no existen en el sistema
     * @throws UsuarioNoEncontradoException si el usuario asignado como responsable no existe
     */
    @Transactional
    public TicketResponseDTO crearTicket(TicketRequestDTO request) {
        if (request == null) {
            throw new IllegalArgumentException("La solicitud de creación de ticket no puede ser nula");
        }

        // 1. Validar la existencia defensiva de las relaciones mandatorias
        Cliente cliente = clienteRepository.findById(request.getClienteId())
                .orElseThrow(() -> new IllegalArgumentException("El cliente con ID " + request.getClienteId() + " no existe"));

        Equipo equipo = equipoRepository.findById(request.getEquipoId())
                .orElseThrow(() -> new IllegalArgumentException("El equipo médico con ID " + request.getEquipoId() + " no existe"));

        Usuario responsable = usuarioRepository.findById(request.getResponsableId())
                .orElseThrow(() -> new UsuarioNoEncontradoException("Usuario responsable no encontrado con ID: " + request.getResponsableId()));

        LocalDateTime ahora = LocalDateTime.now();

        // 2. Generación segura del Folio institucional único (Format: TK-yyyyMMddHHmmssSSS)
        String timestamp = ahora.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
        String folio = "TK-" + timestamp;

        // 3. Cálculo de SLA en capa de servicio (Ejemplo: 24h, 48h, 72h)
        LocalDateTime fechaVencimientoSla = calcularSlaPorPrioridad(request.getPrioridad(), ahora);

        // 4. Instanciar la entidad utilizando el CONSTRUCTOR RICO de dominio
        // Esto automáticamente setea estado=ABIERTO y fechaCreacion=ahora
        Ticket ticket = new Ticket(
                folio,
                request.getTipo(),
                request.getPrioridad(),
                request.getDescripcionInicial(),
                cliente,
                equipo,
                fechaVencimientoSla
        );

        // 5. Asignar el responsable
        ticket.setResponsable(responsable);

        // 6. Persistencia física en base de datos
        Ticket ticketGuardado = ticketRepository.save(ticket);

        // 7. Retorno desacoplado mediante transformación manual
        return ticketMapper.toResponseDTO(ticketGuardado);
    }

    /**
     * Método auxiliar para calcular la fecha de vencimiento del SLA basado en la prioridad.
     * Delegar esto al servicio evita acoplar reglas de tiempos estáticas a la entidad.
     * @param prioridad nivel de prioridad del ticket
     * @param fechaCreacion fecha base de cálculo
     * @return fecha límite del SLA
     */
    private LocalDateTime calcularSlaPorPrioridad(PrioridadTicket prioridad, LocalDateTime fechaCreacion) {

        return switch (prioridad.getNivel()) {
            case 1 -> fechaCreacion.plusHours(24); // ALTA: 24 horas
            case 2 -> fechaCreacion.plusHours(48); // MEDIA: 48 horas
            case 3 -> fechaCreacion.plusHours(72); // BAJA: 72 horas
            default -> fechaCreacion.plusHours(48); // Fallback de seguridad
        };
    }

    /**
     * Recupera un listado paginado de tickets aplicando filtros estrictos de visibilidad
     * basados en el rol y la asignación del usuario solicitante.
     * <p>
     * Reglas de negocio de seguridad implementadas:
     * <ul>
     * <li>ADMIN: Visualiza de forma irrestricta todos los registros de la plataforma.</li>
     * <li>VENTAS: Visualiza únicamente sus tickets asignados que sean del área de VENTA.</li>
     * <li>TECNICO: Visualiza únicamente sus tickets asignados que sean del área de SERVICIO.</li>
     * </ul>
     * </p>
     *
     * @param usuarioLogueado la entidad del usuario que mantiene la sesión activa en Spring Security
     * @param pageable        configuración de paginación e indexación provista por el controlador
     * @return página de DTOs mapeados de forma segura
     */
    @Transactional(readOnly = true)
    public Page<TicketResponseDTO> listarTicketsPorRol(Usuario usuarioLogueado, Pageable pageable) {
        if (usuarioLogueado == null) {
            throw new IllegalArgumentException("El contexto de usuario autenticado es requerido");
        }

        Page<Ticket> ticketPage;

        switch (usuarioLogueado.getRol()) {
            case ADMIN:
                ticketPage = ticketRepository.findAll(pageable);
                break;

            case VENTAS:
                // v2 ELIMINAMOS el filtro de estados activos para permitir ver el histórico completo de VENTAS
                ticketPage = ticketRepository.findByTipoAndResponsableId(
                        TipoTicket.VENTA,
                        usuarioLogueado.getId(),
                        pageable
                );
                break;

            case TECNICO:
                // v2 ELIMINAMOS el filtro de estados activos para permitir ver el histórico completo de TECNICOS
                ticketPage = ticketRepository.findByTipoAndResponsableId(
                        TipoTicket.SERVICIO,
                        usuarioLogueado.getId(),
                        pageable
                );
                break;

            default:
                throw new IllegalStateException("Rol sin políticas de visibilidad asignadas");
        }

        return ticketPage.map(ticketMapper::toResponseDTO);
    }

    /**
     * Recupera tickets filtrados de forma estricta por un estado de negocio o por estatus de SLA.
     */
    @Transactional(readOnly = true)
    public Page<TicketResponseDTO> listarTicketsFiltradosPorRol(String filtro, Usuario usuarioLogueado, Pageable pageable) {
        Page<Ticket> ticketPage;

        // Determinamos si es un filtro de rol base
        Long respId = (usuarioLogueado.getRol() == Rol.ADMIN) ? null : usuarioLogueado.getId();
        TipoTicket tipo = (usuarioLogueado.getRol() == Rol.VENTAS) ? TipoTicket.VENTA :
                (usuarioLogueado.getRol() == Rol.TECNICO) ? TipoTicket.SERVICIO : null;

        if ("VENCIDO".equalsIgnoreCase(filtro)) {
            // Consulta para tickets cuyo SLA expiró y siguen activos
            ticketPage = ticketRepository.findVencidosPorFiltro(respId, tipo, pageable);
        } else {
            // Filtro por EstadoTicket normal (ABIERTO, EN_PROCESO, CERRADO)
            EstadoTicket estado = EstadoTicket.valueOf(filtro.toUpperCase());
            ticketPage = ticketRepository.findPorEstadoRolYResponsable(estado, respId, tipo, pageable);
        }

        return ticketPage.map(ticketMapper::toResponseDTO);
    }

    /**
     * Recupera el detalle atómico de un Ticket mediante su folio de búsqueda.
     *
     * @param folio cadena de caracteres única del ticket
     * @return el DTO de respuesta estructurado
     * @throws IllegalArgumentException si el folio no es localizado en la persistencia
     */
    @Transactional(readOnly = true)
    public TicketResponseDTO obtenerPorFolio(String folio) {
        Ticket ticket = ticketRepository.findByFolio(folio)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró ningún ticket asociado al folio: " + folio));
        return ticketMapper.toResponseDTO(ticket);
    }

    /**
     * Busca tickets por folio parcial respetando los filtros de seguridad por Rol corporativo.
     */
    @Transactional(readOnly = true)
    public Page<TicketResponseDTO> buscarTicketsPorFolioYRol(String folio, Usuario usuarioLogueado, Pageable pageable) {
        String queryFolio = "%" + folio.trim().toUpperCase() + "%";
        Page<Ticket> ticketPage;

        switch (usuarioLogueado.getRol()) {
            case ADMIN:
                ticketPage = ticketRepository.findByFolioContainingIgnoreCase(queryFolio, pageable);
                break;
            case VENTAS:
                ticketPage = ticketRepository.findByFolioAndTipoAndResponsable(queryFolio, TipoTicket.VENTA, usuarioLogueado.getId(), pageable);
                break;
            case TECNICO:
                ticketPage = ticketRepository.findByFolioAndTipoAndResponsable(queryFolio, TipoTicket.SERVICIO, usuarioLogueado.getId(), pageable);
                break;
            default:
                throw new IllegalStateException("Rol no soportado");
        }
        return ticketPage.map(ticketMapper::toResponseDTO);
    }

    /**
     * Recupera el detalle atómico de un Ticket mediante su clave primaria.
     *
     * @param id identificador único secuencial
     * @return el DTO de respuesta estructurado
     * @throws IllegalArgumentException si el identificador no es localizado en la persistencia
     */
    @Transactional(readOnly = true)
    public TicketResponseDTO obtenerPorId(Long id) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró ningún ticket con el ID: " + id));
        return ticketMapper.toResponseDTO(ticket);
    }

    /**
     * Resuelve de forma unificada las combinaciones de filtros de la bandeja de entrada.
     */
    @Transactional(readOnly = true)
    public Page<TicketResponseDTO> listarTicketsCombinados(String folio, String filtro, Usuario usuarioLogueado, Pageable pageable) {

        Long responsableId = (usuarioLogueado.getRol() == Rol.ADMIN) ? null : usuarioLogueado.getId();
        TipoTicket tipo = (usuarioLogueado.getRol() == Rol.VENTAS) ? TipoTicket.VENTA :
                (usuarioLogueado.getRol() == Rol.TECNICO) ? TipoTicket.SERVICIO : null;

        EstadoTicket estado = null;
        boolean esVencido = false;

        if (filtro != null) {
            if ("VENCIDO".equalsIgnoreCase(filtro)) {
                esVencido = true;
            } else {
                try {
                    estado = EstadoTicket.valueOf(filtro);
                } catch (IllegalArgumentException e) {
                    // Evita quiebres si el string no coincide
                }
            }
        }

        String folioQuery = (folio != null && !folio.trim().isEmpty()) ? "%" + folio.trim().toUpperCase() + "%" : null;

        Page<Ticket> pagina = ticketRepository.findTicketsCombinados(folioQuery, estado, tipo, responsableId, esVencido, pageable);
        return pagina.map(ticketMapper::toResponseDTO);
    }

    /**
     * Inicia la atención operativa de un ticket, transicionándolo a EN_PROCESO.
     * @param id identificador único del ticket
     * @throws IllegalArgumentException si el ticket no existe
     * @throws IllegalStateException si el ticket ya está cerrado
     */
    @Transactional
    public void iniciarAtencion(Long id) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró el ticket con ID: " + id));

        // El método setEstado de la entidad ya tiene la validación defensiva del flujo
        ticket.setEstado(EstadoTicket.EN_PROCESO);
    }

    /**
     * Cierra un ticket operativamente y registra el folio del documento generado (Cotización u Orden de Servicio).
     *
     * @param id identificador único del ticket
     * @param folioDocumento el folio ingresado manualmente por el usuario
     * @throws IllegalArgumentException si el ticket no existe o el folio es inválido
     */
    @Transactional
    public void cerrarTicket(Long id, String folioDocumento) {
        if (folioDocumento == null || folioDocumento.trim().isEmpty()) {
            throw new IllegalArgumentException("El folio del documento es obligatorio para cerrar el ticket");
        }

        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró el ticket con ID: " + id));

        // Transicionamos a CERRADO. La entidad asignará automáticamente la fecha_cierre
        ticket.setEstado(EstadoTicket.CERRADO);

        // Guardamos el folio en la columna correcta dependiendo del área (Ventas o Técnico)
        if (ticket.getTipo() == TipoTicket.VENTA) {
            ticket.setFolioCotizacion(folioDocumento.trim());
        } else {
            ticket.setFolioOrdenServicio(folioDocumento.trim());
        }
    }

    /**
     * Agrega un comentario inmutable al historial de un ticket.
     *
     * @param ticketId identificador del ticket
     * @param emailAutor email del usuario extraído del contexto de seguridad
     * @param textoComentario contenido del comentario
     */
    @Transactional
    public void agregarComentario(Long ticketId, String emailAutor, String textoComentario) {
        if (textoComentario == null || textoComentario.trim().isEmpty()) {
            throw new IllegalArgumentException("El comentario no puede estar vacío");
        }

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket no encontrado"));

        Usuario autor = usuarioRepository.findByEmail(emailAutor)
                .orElseThrow(() -> new IllegalStateException("Autor no encontrado en el sistema"));

        // Utilizamos el constructor para garantizar un estado válido
        Comentario comentario = new Comentario(ticket, autor, textoComentario.trim());
        comentarioRepository.save(comentario);
    }

    /**
     * Obtiene el historial de comentarios de un ticket mapeado a DTOs.
     *
     * @param ticketId identificador del ticket
     * @return lista inmutable de comentarios estructurados
     */
    @Transactional(readOnly = true)
    public List<ComentarioDTO> obtenerComentariosPorTicket(Long ticketId) {
        return comentarioRepository.findByTicketIdOrderByFechaCreacionAsc(ticketId)
                .stream()
                .map(c -> new ComentarioDTO(
                        c.getId(),
                        c.getTexto(),
                        c.getAutor().getNombre(), // Cargamos solo el nombre del autor
                        c.getFechaCreacion()
                ))
                .toList(); // .toList() produce una lista inmutable
    }

    /**
     * Recupera las métricas de rendimiento consolidadas para el Dashboard del usuario operativo.
     * Realiza una única llamada optimizada a la base de datos persistente.
     *
     * @param username correo electrónico del usuario autenticado
     * @return DashboardStatsDTO con la información lista para el modelo
     */
    @Transactional(readOnly = true)
    public DashboardStatsDTO obtenerTicketsParaDashboard(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre de usuario no puede estar vacío");
        }

        Usuario usuarioLogueado = usuarioRepository.findByEmail(username)
                .orElseThrow(() -> new IllegalStateException("Usuario autenticado no encontrado en BD"));

        // Aplicamos bifurcación por Rol corporativo
        // Si es ADMIN, mandamos null para que la query sume TODO el sistema.
        // Si es VENTAS o TECNICO, pasamos su ID para que solo cuente sus asignaciones.
        Long filtroResponsableId = (usuarioLogueado.getRol() == Rol.ADMIN) ? null : usuarioLogueado.getId();

        return ticketRepository.obtenerEstadisticasDashboard(
                filtroResponsableId,
                EstadoTicket.ABIERTO,
                EstadoTicket.EN_PROCESO,
                EstadoTicket.CERRADO
        );
    }
}