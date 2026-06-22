package com.biometec.mesacontrol.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * Entidad que representa un ticket de servicio o venta en la plataforma Mesa de Control.
 *
 * Un ticket es la unidad central de trabajo en el sistema que:
 * - Registra solicitudes de servicio técnico o transacciones de venta
 * - Asocia un cliente con sus equipos médicos
 * - Rastrea el ciclo de vida completo (ABIERTO → EN_PROCESO → CERRADO)
 * - Gestiona SLAs y tiempos de respuesta
 * - Permite asignación a usuarios responsables
 *
 * Reglas de Negocio:
 * - Todo ticket debe tener cliente y equipo asociados (no nulos)
 * - El responsable es opcional en la creación pero recomendado en EN_PROCESO
 * - La fecha de vencimiento de SLA debe ser posterior a la creación
 * - Las transiciones de estado son unidireccionales: ABIERTO → EN_PROCESO → CERRADO
 * - Al cerrar, se establece automáticamente la fecha de cierre
 * - Folio debe ser único y cumplir formato específico
 *
 * Relaciones:
 * - ManyToOne con Cliente: cada ticket pertenece a un cliente
 * - ManyToOne con Equipo: cada ticket está asociado a un equipo específico
 * - ManyToOne con Usuario: asignación opcional de responsable
 *
 * Estrategia de equals/hashCode Defensiva:
 * - Usa ID para objetos persistidos (garantizado único)
 * - Usa folio para objetos transientes (también único)
 * - Sigue patrón recomendado por Hibernate para evitar problemas en colecciones
 *
 * @author Juan Jaramillo
 * @version 1.0
 */
@Entity
@Table(name = "ticket", indexes = {
        @Index(name = "idx_ticket_folio", columnList = "folio", unique = true),
        @Index(name = "idx_ticket_cliente_id", columnList = "cliente_id"),
        @Index(name = "idx_ticket_equipo_id", columnList = "equipo_id"),
        @Index(name = "idx_ticket_estado", columnList = "estado"),
        @Index(name = "idx_ticket_prioridad", columnList = "prioridad"),
        @Index(name = "idx_ticket_responsable_id", columnList = "responsable_id")
})
public class Ticket {

    /**
     * Identificador único del ticket.
     * Generado automáticamente por la base de datos usando IDENTITY.
     * Se utiliza como clave primaria y como base para equals/hashCode en objetos persistidos.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Folio único que identifica el ticket de forma legible.
     * Formato recomendado: TKT-YYYYMMDD-XXXXX
     * Ejemplo: TKT-20260608-00001
     *
     * Requerido y no puede estar vacío.
     * Debe cumplir con restricción UNIQUE en la base de datos.
     * Se normaliza a mayúsculas para consistencia.
     */
    @Column(nullable = false, length = 50, unique = true, name = "folio")
    private String folio;

    /**
     * Tipo de ticket que categoriza la naturaleza del trabajo.
     * Valores posibles:
     * - VENTA: Transacciones de venta de servicios o productos
     * - SERVICIO: Servicios técnicos, mantenimiento, soporte
     *
     * Requerido. Se almacena como STRING en base de datos.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50, name = "tipo")
    private TipoTicket tipo;

    /**
     * Prioridad del ticket que determina urgencia y orden de procesamiento.
     * Valores posibles:
     * - ALTA (nivel 1): Problemas críticos, requiere atención inmediata
     * - MEDIA (nivel 2): Problemas moderados, procesamiento estándar
     * - BAJA (nivel 3): Problemas menores, puede esperar
     *
     * Requerido. Se almacena como STRING en base de datos.
     * Se utiliza para ordenamiento de tickets pendientes.
     */
    @Column(nullable = false, name = "prioridad")
    private PrioridadTicket prioridad;

    /**
     * Estado actual del ticket en su ciclo de vida.
     * Valores posibles:
     * - ABIERTO: Ticket recién creado, pendiente de procesamiento
     * - EN_PROCESO: Ticket siendo trabajado activamente
     * - CERRADO: Ticket finalizado y cerrado
     *
     * Requerido. Se almacena como STRING en base de datos.
     * Las transiciones son unidireccionales: ABIERTO → EN_PROCESO → CERRADO
     * No se permite ir hacia atrás o saltar estados.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50, name = "estado")
    private EstadoTicket estado;

    /**
     * Descripción detallada del problema o solicitud inicial.
     * Contiene información crítica para entender la naturaleza del ticket.
     *
     * Requerido y no puede estar vacío.
     * Se almacena como TEXT para permitir contenido extenso.
     * No se debe modificar una vez creado (es histórico).
     */
    @Column(nullable = false, columnDefinition = "TEXT", name = "descripcion_inicial")
    private String descripcionInicial;

    /**
     * Fecha y hora de creación del ticket en UTC.
     * Se establece automáticamente al momento de instanciar el objeto.
     * Se utiliza como referencia para calcular SLAs y métricas.
     *
     * Requerido. No debe ser modificado después de la creación.
     */
    @Column(nullable = false, name = "fecha_creacion")
    private LocalDateTime fechaCreacion;

    /**
     * Fecha y hora de vencimiento del Service Level Agreement (SLA).
     * Define el plazo máximo para resolver el ticket según su tipo y prioridad.
     * Debe ser posterior a la fecha de creación.
     *
     * Se utiliza para:
     * - Alertas de incumplimiento
     * - Reportes de SLA
     * - Priorización de tickets
     *
     * Requerido y no puede ser nulo.
     */
    @Column(nullable = false, name = "fecha_vencimiento_sla")
    private LocalDateTime fechaVencimientoSla;

    /**
     * Fecha y hora de cierre del ticket.
     * Se establece automáticamente cuando el ticket es marcado como CERRADO.
     *
     * Opcional (puede ser nulo si el ticket no ha sido cerrado).
     * Se utiliza para calcular:
     * - Tiempo de resolución
     * - Cumplimiento de SLA
     * - Métricas de eficiencia
     */
    @Column(nullable = true, name = "fecha_cierre")
    private LocalDateTime fechaCierre;

    /**
     * Folio de cotización asociado al ticket (si aplica).
     * Referencia a documentos de cotización generados como resultado del ticket.
     * Formato: cadena alfanumérica según políticas organizacionales.
     *
     * Opcional. Puede ser nulo si no hay cotización generada.
     * Útil para rastrear documentos relacionados.
     */
    @Column(nullable = true, length = 100, name = "folio_cotizacion")
    private String folioCotizacion;

    /**
     * Folio de orden de servicio asociado al ticket (si aplica).
     * Referencia a órdenes de servicio generadas como resultado del ticket.
     * Formato: cadena alfanumérica según políticas organizacionales.
     *
     * Opcional. Puede ser nulo si no hay orden de servicio generada.
     * Útil para vinculación con sistemas de ejecución.
     */
    @Column(nullable = true, length = 100, name = "folio_orden_servicio")
    private String folioOrdenServicio;

    /**
     * Cliente propietario del ticket.
     * Relación Many-to-One: múltiples tickets pueden pertenecer al mismo cliente.
     *
     * Requerido. No puede ser nulo.
     * Se carga con estrategia LAZY para optimizar consultas.
     * Se utiliza para filtrado y generación de reportes por cliente.
     *
     * Restricción de integridad referencial: FOREIGN KEY
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "cliente_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_ticket_cliente_id",
                    value = ConstraintMode.CONSTRAINT
            )
    )
    private Cliente cliente;

    /**
     * Equipo médico asociado al ticket.
     * Relación Many-to-One: múltiples tickets pueden estar asociados al mismo equipo.
     *
     * Requerido. No puede ser nulo.
     * Se carga con estrategia LAZY para optimizar consultas.
     * Debe pertenecer al mismo cliente del ticket (validar en lógica de negocio).
     * Se utiliza para rastrear historiales de equipos.
     *
     * Restricción de integridad referencial: FOREIGN KEY
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "equipo_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_ticket_equipo_id",
                    value = ConstraintMode.CONSTRAINT
            )
    )
    private Equipo equipo;

    /**
     * Usuario responsable de la ejecución/seguimiento del ticket.
     * Relación Many-to-One: múltiples tickets pueden ser responsabilidad del mismo usuario.
     *
     * Opcional (puede ser nulo). Un ticket puede crearse sin responsable asignado.
     * Se recomienda asignar responsable cuando el ticket entra en EN_PROCESO.
     * Se carga con estrategia LAZY para optimizar consultas.
     * Se utiliza para filtrado de carga de trabajo y reportes de productividad.
     *
     * Restricción de integridad referencial: FOREIGN KEY (nullable)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsable_id", nullable = false, foreignKey = @ForeignKey(name = "fk_ticket_usuario"))
    private Usuario responsable;

    // ========================
    // CONSTRUCTORES
    // ========================

    /**
     * Constructor vacío requerido por JPA.
     * No debe ser usado directamente en la lógica de negocio.
     */
    public Ticket() {
    }

    /**
     * Constructor completo para creación de nuevo ticket.
     * Realiza validaciones defensivas de todos los parámetros.
     *
     * Inicializa automáticamente:
     * - estado = ABIERTO
     * - fechaCreacion = LocalDateTime.now()
     * - responsable = null (a asignar posteriormente)
     * - fechaCierre = null (a establecer al cerrar)
     *
     * @param folio folio único del ticket (formato: TKT-YYYYMMDD-XXXXX)
     * @param tipo tipo de ticket (VENTA o SERVICIO)
     * @param prioridad prioridad del ticket (ALTA, MEDIA, BAJA)
     * @param descripcionInicial descripción detallada del problema
     * @param cliente cliente propietario (no puede ser nulo)
     * @param equipo equipo asociado (no puede ser nulo)
     * @param fechaVencimientoSla fecha de vencimiento del SLA (posterior a ahora)
     *
     * @throws IllegalArgumentException si algún parámetro es nulo o inválido
     */
    public Ticket(String folio, TipoTicket tipo, PrioridadTicket prioridad,
                  String descripcionInicial, Cliente cliente, Equipo equipo,
                  LocalDateTime fechaVencimientoSla) {
        this.setFolio(folio);
        this.setTipo(tipo);
        this.setPrioridad(prioridad);
        this.setDescripcionInicial(descripcionInicial);
        this.setCliente(cliente);
        this.setEquipo(equipo);

        this.estado = EstadoTicket.ABIERTO;
        this.fechaCreacion = LocalDateTime.now();
        this.setFechaVencimientoSla(fechaVencimientoSla);

        this.responsable = null;
        this.fechaCierre = null;
    }

    // ========================
    // GETTERS Y SETTERS
    // ========================

    /**
     * Obtiene el identificador único del ticket.
     *
     * @return el ID del ticket, null si es transiente
     */
    public Long getId() {
        return id;
    }

    /**
     * Establece el identificador único del ticket.
     * Principalmente para uso interno de JPA/Hibernate.
     *
     * @param id el ID del ticket
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Obtiene el folio único del ticket.
     *
     * @return el folio del ticket (nunca nulo)
     */
    public String getFolio() {
        return folio;
    }

    /**
     * Establece el folio único del ticket.
     * Valida que no sea nulo, vacío ni tenga espacios innecesarios.
     * Se normaliza a mayúsculas para consistencia.
     *
     * @param folio el folio del ticket
     * @throws IllegalArgumentException si el folio está vacío, es nulo o tiene formato inválido
     */
    public void setFolio(String folio) {
        if (folio == null || folio.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "El folio del ticket no puede estar vacío. " +
                            "Formato recomendado: TKT-YYYYMMDD-XXXXX");
        }
        this.folio = folio.trim().toUpperCase();
    }

    /**
     * Obtiene el tipo de ticket.
     *
     * @return el tipo del ticket (VENTA o SERVICIO)
     */
    public TipoTicket getTipo() {
        return tipo;
    }

    /**
     * Establece el tipo de ticket.
     * Valida que no sea nulo.
     *
     * @param tipo el tipo del ticket (VENTA o SERVICIO)
     * @throws IllegalArgumentException si el tipo es nulo
     */
    public void setTipo(TipoTicket tipo) {
        if (tipo == null) {
            throw new IllegalArgumentException(
                    "El tipo de ticket no puede ser nulo. " +
                            "Valores válidos: VENTA, SERVICIO");
        }
        this.tipo = tipo;
    }

    /**
     * Obtiene la prioridad del ticket.
     *
     * @return la prioridad del ticket (ALTA, MEDIA, BAJA)
     */
    public PrioridadTicket getPrioridad() {
        return prioridad;
    }

    /**
     * Establece la prioridad del ticket.
     * Valida que no sea nula.
     *
     * @param prioridad la prioridad del ticket (ALTA, MEDIA, BAJA)
     * @throws IllegalArgumentException si la prioridad es nula
     */
    public void setPrioridad(PrioridadTicket prioridad) {
        if (prioridad == null) {
            throw new IllegalArgumentException(
                    "La prioridad del ticket no puede ser nula. " +
                            "Valores válidos: ALTA, MEDIA, BAJA");
        }
        this.prioridad = prioridad;
    }

    /**
     * Obtiene el estado actual del ticket.
     *
     * @return el estado del ticket (ABIERTO, EN_PROCESO, CERRADO)
     */
    public EstadoTicket getEstado() {
        return estado;
    }

    /**
     * Cambia el estado del ticket con validación stricta de transiciones.
     * Solo permite transiciones válidas según el flujo: ABIERTO → EN_PROCESO → CERRADO
     *
     * Behavior especial al cerrar:
     * - Establece automáticamente fechaCierre = LocalDateTime.now()
     * - No permite volver a estados anteriores
     *
     * @param nuevoEstado el nuevo estado a transicionar
     * @throws IllegalArgumentException si el nuevo estado es nulo
     * @throws IllegalStateException si la transición no es válida
     */
    public void setEstado(EstadoTicket nuevoEstado) {
        if (nuevoEstado == null) {
            throw new IllegalArgumentException(
                    "El estado del ticket no puede ser nulo. " +
                            "Valores válidos: ABIERTO, EN_PROCESO, CERRADO");
        }

        if (!this.estado.esTransicionValida(nuevoEstado)) {
            throw new IllegalStateException(
                    String.format(
                            "No es posible transicionar de estado %s a %s. " +
                                    "Flujo permitido: ABIERTO → EN_PROCESO → CERRADO",
                            this.estado.name(), nuevoEstado.name()
                    )
            );
        }

        this.estado = nuevoEstado;

        if (nuevoEstado == EstadoTicket.CERRADO) {
            this.fechaCierre = LocalDateTime.now();
        }
    }

    /**
     * Obtiene la descripción inicial del ticket.
     *
     * @return la descripción inicial (nunca nulo)
     */
    public String getDescripcionInicial() {
        return descripcionInicial;
    }

    /**
     * Establece la descripción inicial del ticket.
     * Valida que no sea nula o vacía.
     * Se recomienda no modificar después de creación (es histórico).
     *
     * @param descripcionInicial la descripción inicial
     * @throws IllegalArgumentException si la descripción está vacía o es nula
     */
    public void setDescripcionInicial(String descripcionInicial) {
        if (descripcionInicial == null || descripcionInicial.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "La descripción inicial no puede estar vacía. " +
                            "Proporcione una descripción detallada del problema.");
        }
        this.descripcionInicial = descripcionInicial.trim();
    }

    /**
     * Obtiene la fecha de creación del ticket.
     *
     * @return la fecha de creación en UTC (nunca nulo)
     */
    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    /**
     * Establece la fecha de creación del ticket.
     * Uso interno (principalmente para JPA/Hibernate).
     * No debe ser modificado después de la creación en la lógica de negocio.
     *
     * @param fechaCreacion la fecha de creación
     */
    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    /**
     * Obtiene la fecha de vencimiento del SLA.
     *
     * @return la fecha de vencimiento del SLA (nunca nulo)
     */
    public LocalDateTime getFechaVencimientoSla() {
        return fechaVencimientoSla;
    }

    /**
     * Establece la fecha de vencimiento del SLA.
     * Valida que sea posterior a la fecha de creación.
     *
     * Reglas de validación:
     * - No puede ser nulo
     * - Debe ser posterior a fechaCreacion
     * - Se utiliza para cálculos de SLA
     *
     * @param fechaVencimientoSla la fecha de vencimiento del SLA
     * @throws IllegalArgumentException si la fecha es nula o anterior a la creación
     */
    public void setFechaVencimientoSla(LocalDateTime fechaVencimientoSla) {
        if (fechaVencimientoSla == null) {
            throw new IllegalArgumentException(
                    "La fecha de vencimiento del SLA no puede ser nula.");
        }

        if (this.fechaCreacion != null && fechaVencimientoSla.isBefore(this.fechaCreacion)) {
            throw new IllegalArgumentException(
                    String.format(
                            "La fecha de vencimiento del SLA (%s) no puede ser anterior " +
                                    "a la fecha de creación (%s).",
                            fechaVencimientoSla, this.fechaCreacion
                    )
            );
        }

        this.fechaVencimientoSla = fechaVencimientoSla;
    }

    /**
     * Obtiene la fecha de cierre del ticket.
     *
     * @return la fecha de cierre (null si el ticket no está cerrado)
     */
    public LocalDateTime getFechaCierre() {
        return fechaCierre;
    }

    /**
     * Establece la fecha de cierre del ticket.
     * Uso interno (principalmente establecido automáticamente al cerrar).
     *
     * @param fechaCierre la fecha de cierre
     */
    public void setFechaCierre(LocalDateTime fechaCierre) {
        this.fechaCierre = fechaCierre;
    }

    /**
     * Obtiene el folio de cotización asociado.
     *
     * @return el folio de cotización (puede ser nulo)
     */
    public String getFolioCotizacion() {
        return folioCotizacion;
    }

    /**
     * Establece el folio de cotización.
     * Opcional. Se normaliza eliminando espacios innecesarios.
     *
     * @param folioCotizacion el folio de cotización (puede ser nulo)
     */
    public void setFolioCotizacion(String folioCotizacion) {
        this.folioCotizacion = folioCotizacion != null ? folioCotizacion.trim() : null;
    }

    /**
     * Obtiene el folio de orden de servicio asociado.
     *
     * @return el folio de orden de servicio (puede ser nulo)
     */
    public String getFolioOrdenServicio() {
        return folioOrdenServicio;
    }

    /**
     * Establece el folio de orden de servicio.
     * Opcional. Se normaliza eliminando espacios innecesarios.
     *
     * @param folioOrdenServicio el folio de orden de servicio (puede ser nulo)
     */
    public void setFolioOrdenServicio(String folioOrdenServicio) {
        this.folioOrdenServicio = folioOrdenServicio != null ? folioOrdenServicio.trim() : null;
    }

    /**
     * Obtiene el cliente propietario del ticket.
     *
     * @return el cliente del ticket (nunca nulo)
     */
    public Cliente getCliente() {
        return cliente;
    }

    /**
     * Establece el cliente propietario del ticket.
     * Valida que no sea nulo.
     *
     * @param cliente el cliente del ticket
     * @throws IllegalArgumentException si el cliente es nulo
     */
    public void setCliente(Cliente cliente) {
        if (cliente == null) {
            throw new IllegalArgumentException(
                    "El cliente del ticket no puede ser nulo. " +
                            "Cada ticket debe estar asociado a un cliente válido.");
        }
        this.cliente = cliente;
    }

    /**
     * Obtiene el equipo asociado al ticket.
     *
     * @return el equipo del ticket (nunca nulo)
     */
    public Equipo getEquipo() {
        return equipo;
    }

    /**
     * Establece el equipo asociado al ticket.
     * Valida que no sea nulo y que pertenezca al mismo cliente del ticket.
     *
     * @param equipo el equipo del ticket
     * @throws IllegalArgumentException si el equipo es nulo o no pertenece al cliente
     */
    public void setEquipo(Equipo equipo) {
        if (equipo == null) {
            throw new IllegalArgumentException(
                    "El equipo del ticket no puede ser nulo. " +
                            "Cada ticket debe estar asociado a un equipo válido.");
        }
        this.equipo = equipo;
    }

    /**
     * Obtiene el usuario responsable del ticket.
     *
     * @return el responsable del ticket (puede ser nulo)
     */
    public Usuario getResponsable() {
        return responsable;
    }

    /**
     * Establece el usuario responsable del ticket.
     * Opcional. Puede ser nulo si el ticket no ha sido asignado.
     *
     * @param responsable el responsable del ticket (puede ser nulo)
     */
    public void setResponsable(Usuario responsable) {
        this.responsable = responsable;
    }

    // ========================
    // MÉTODOS DE LÓGICA DE NEGOCIO
    // ========================

    /**
     * Verifica si el SLA del ticket está vencido.
     * Un ticket cerrado nunca está vencido (se cumplió o no pero ya no hay SLA activo).
     *
     * @return true si fecha actual > fecha_vencimiento_sla y estado != CERRADO
     */
    public boolean estaSLAVencido() {
        if (this.estado == EstadoTicket.CERRADO) {
            return false;
        }
        return LocalDateTime.now().isAfter(this.fechaVencimientoSla);
    }

    /**
     * Calcula los días restantes hasta el vencimiento del SLA.
     * Retorna número negativo si ya está vencido.
     *
     * @return días restantes (negativo si vencido)
     */
    public Long diasRestantesSLA() {
        return ChronoUnit.DAYS.between(LocalDateTime.now(), this.fechaVencimientoSla);
    }

    /**
     * Calcula las horas restantes hasta el vencimiento del SLA.
     * Más preciso que días para SLAs cortos.
     *
     * @return horas restantes (negativo si vencido)
     */
    public Long horasRestantesSLA() {
        return ChronoUnit.HOURS.between(LocalDateTime.now(), this.fechaVencimientoSla);
    }

    /**
     * Verifica si el ticket puede ser cerrado.
     * Un ticket puede cerrarse si no está en estado CERRADO.
     *
     * @return true si el ticket está en ABIERTO o EN_PROCESO
     */
    public boolean puedeSerCerrado() {
        return this.estado != EstadoTicket.CERRADO;
    }

    /**
     * Asigna un responsable al ticket y lo marca como EN_PROCESO si es necesario.
     *
     * Reglas:
     * - El usuario no puede ser nulo
     * - No se puede asignar responsable a tickets cerrados
     * - Si está ABIERTO, cambia automáticamente a EN_PROCESO
     *
     * @param usuario el usuario responsable del ticket
     * @throws IllegalArgumentException si el usuario es nulo
     * @throws IllegalStateException si el ticket está cerrado
     */
    public void asignarResponsable(Usuario usuario) {
        if (usuario == null) {
            throw new IllegalArgumentException(
                    "El usuario responsable no puede ser nulo. " +
                            "Proporcione un usuario válido para asignar el ticket.");
        }

        if (this.estado == EstadoTicket.CERRADO) {
            throw new IllegalStateException(
                    "No se puede asignar responsable a un ticket en estado CERRADO. " +
                            "El ticket ya no puede ser modificado.");
        }

        this.responsable = usuario;

        if (this.estado == EstadoTicket.ABIERTO) {
            this.estado = EstadoTicket.EN_PROCESO;
        }
    }

    /**
     * Calcula el tiempo transcurrido desde la creación del ticket.
     * Útil para métricas de tiempo de resolución.
     *
     * @return tiempo en minutos desde la creación
     */
    public Long minutosDesdeCreacion() {
        return ChronoUnit.MINUTES.between(this.fechaCreacion, LocalDateTime.now());
    }

    /**
     * Calcula el tiempo total de resolución del ticket (si está cerrado).
     *
     * @return tiempo en minutos desde creación hasta cierre (null si no está cerrado)
     */
    public Long minutosParaResolver() {
        if (this.fechaCierre == null) {
            return null;
        }
        return ChronoUnit.MINUTES.between(this.fechaCreacion, this.fechaCierre);
    }

    /**
     * Verifica si el SLA será cumplido si el ticket se cierra ahora.
     *
     * @return true si LocalDateTime.now() <= fechaVencimientoSla
     */
    public boolean cumpliriaSLASiSeClerra() {
        return LocalDateTime.now().isBefore(this.fechaVencimientoSla) ||
                LocalDateTime.now().isEqual(this.fechaVencimientoSla);
    }

    // ========================
    // EQUALS Y HASHCODE
    // ========================

    /**
     * Compara dos tickets de forma defensiva siguiendo recomendaciones de Hibernate.
     *
     * Estrategia de comparación:
     * 1. Si ambos objetos tienen ID (están persistidos), compara por ID
     * 2. Si alguno es transiente (sin ID), compara por folio (también único)
     * 3. Evita problemas con lazy loading y detached entities
     *
     * @param o el objeto a comparar
     * @return true si son el mismo ticket
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Ticket ticket = (Ticket) o;

        // Si ambos tienen ID, comparar por ID
        if (this.id != null && ticket.id != null) {
            return Objects.equals(this.id, ticket.id);
        }

        // Si alguno es transiente, comparar por folio (único)
        if (this.folio != null && ticket.folio != null) {
            return Objects.equals(this.folio, ticket.folio);
        }

        return false;
    }

    /**
     * Genera el código hash del ticket de forma defensiva.
     *
     * Estrategia:
     * 1. Si tiene ID (persistido), usa el ID → garantizado único
     * 2. Si es transiente, usa el folio → también único
     * 3. Esto asegura que el hashCode es consistente con equals()
     *
     * @return hash code del ticket
     */
    @Override
    public int hashCode() {
        if (this.id != null) {
            return Objects.hash(id);
        }

        if (this.folio != null) {
            return Objects.hash(folio);
        }

        return System.identityHashCode(this);
    }

    // ========================
    // TOSTRING
    // ========================

    /**
     * Representación en string del ticket para debugging.
     * Incluye información completa pero sin cargar relaciones lazy.
     */
    @Override
    public String toString() {
        return "Ticket{" +
                "id=" + id +
                ", folio='" + folio + '\'' +
                ", tipo=" + tipo +
                ", prioridad=" + prioridad +
                ", estado=" + estado +
                ", fechaCreacion=" + fechaCreacion +
                ", fechaVencimientoSla=" + fechaVencimientoSla +
                ", fechaCierre=" + fechaCierre +
                ", cliente_id=" + (cliente != null ? cliente.getId() : "null") +
                ", equipo_id=" + (equipo != null ? equipo.getId() : "null") +
                ", responsable_id=" + (responsable != null ? responsable.getId() : "null") +
                '}';
    }

}