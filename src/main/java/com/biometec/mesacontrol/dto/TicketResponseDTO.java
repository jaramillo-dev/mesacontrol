package com.biometec.mesacontrol.dto;

import com.biometec.mesacontrol.entity.EstadoTicket;
import com.biometec.mesacontrol.entity.PrioridadTicket;
import com.biometec.mesacontrol.entity.TipoTicket;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * DTO de lectura utilizado para representar la información completa
 * de un ticket dentro de las vistas de consulta y seguimiento.
 *
 * <p>
 * Consolida datos provenientes del ticket, cliente, equipo y responsable
 * para evitar exponer directamente entidades JPA a la capa web.
 * </p>
 *
 * <p>
 * Además incorpora lógica auxiliar relacionada con la visualización
 * del estado de cumplimiento del SLA.
 * </p>
 */
public class TicketResponseDTO {

    private Long id;
    private String folio;
    private TipoTicket tipo;
    private PrioridadTicket prioridad;
    private EstadoTicket estado;
    private String descripcionInicial;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaVencimientoSla;
    private LocalDateTime fechaCierre;
    private String folioCotizacion;
    private String folioOrdenServicio;
    private Long clienteId;
    private String clienteNombre;
    private Long equipoId;
    private String equipoNombre;
    private String equipoNumeroSerie;
    private Long responsableId;
    private String responsableNombre;

    public TicketResponseDTO() {
    }

    // Getters y Setters Estándar
    public Long getId() { return id; }

    public void setId(Long id) { this.id = id; }

    public String getFolio() { return folio; }

    public void setFolio(String folio) { this.folio = folio; }

    public TipoTicket getTipo() { return tipo; }

    public void setTipo(TipoTicket tipo) { this.tipo = tipo; }

    public PrioridadTicket getPrioridad() { return prioridad; }

    public void setPrioridad(PrioridadTicket prioridad) { this.prioridad = prioridad; }

    public EstadoTicket getEstado() { return estado; }

    public void setEstado(EstadoTicket estado) { this.estado = estado; }

    public String getDescripcionInicial() { return descripcionInicial; }

    public void setDescripcionInicial(String descripcionInicial) { this.descripcionInicial = descripcionInicial; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }

    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public LocalDateTime getFechaVencimientoSla() { return fechaVencimientoSla; }

    public void setFechaVencimientoSla(LocalDateTime fechaVencimientoSla) { this.fechaVencimientoSla = fechaVencimientoSla; }

    public LocalDateTime getFechaCierre() { return fechaCierre; }

    public void setFechaCierre(LocalDateTime fechaCierre) { this.fechaCierre = fechaCierre; }

    public String getFolioCotizacion() { return folioCotizacion; }

    public void setFolioCotizacion(String folioCotizacion) { this.folioCotizacion = folioCotizacion; }

    public String getFolioOrdenServicio() { return folioOrdenServicio; }

    public void setFolioOrdenServicio(String folioOrdenServicio) { this.folioOrdenServicio = folioOrdenServicio; }

    public Long getClienteId() { return clienteId; }

    public void setClienteId(Long clienteId) { this.clienteId = clienteId; }

    public String getClienteNombre() { return clienteNombre; }

    public void setClienteNombre(String clienteNombre) { this.clienteNombre = clienteNombre; }

    public Long getEquipoId() { return equipoId; }

    public void setEquipoId(Long equipoId) { this.equipoId = equipoId; }

    public String getEquipoNombre() { return equipoNombre; }

    public void setEquipoNombre(String equipoNombre) { this.equipoNombre = equipoNombre; }

    public String getEquipoNumeroSerie() { return equipoNumeroSerie; }

    public void setEquipoNumeroSerie(String equipoNumeroSerie) { this.equipoNumeroSerie = equipoNumeroSerie; }

    public Long getResponsableId() { return responsableId; }

    public void setResponsableId(Long responsableId) { this.responsableId = responsableId; }

    public String getResponsableNombre() { return responsableNombre; }

    public void setResponsableNombre(String responsableNombre) { this.responsableNombre = responsableNombre; }

    /**
     * Calcula dinámicamente el estado visual del SLA del ticket.
     *
     * <p>
     * El resultado se utiliza para mostrar un semáforo de seguimiento
     * en la interfaz de usuario:
     * </p>
     *
     * <ul>
     *     <li>VERDE: SLA dentro de parámetros normales.</li>
     *     <li>AMARILLO: consumo superior al 80 % del tiempo disponible.</li>
     *     <li>ROJO: SLA vencido.</li>
     *     <li>FINALIZADO: ticket cerrado.</li>
     * </ul>
     *
     * @return estado visual del SLA
     *
     * @author Juan Jaramillo
     * @version 1.0
     */
    public String getSlaSemaforo() {
        if (this.estado == EstadoTicket.CERRADO) {
            return "FINALIZADO";
        }

        LocalDateTime ahora = LocalDateTime.now();

        if (ahora.isAfter(this.fechaVencimientoSla)) {
            return "ROJO";
        }

        long tiempoTotalMinutos = ChronoUnit.MINUTES.between(this.fechaCreacion, this.fechaVencimientoSla);
        long tiempoTranscurridoMinutos = ChronoUnit.MINUTES.between(this.fechaCreacion, ahora);

        if (tiempoTotalMinutos <= 0) return "ROJO";

        double porcentajeConsumido = ((double) tiempoTranscurridoMinutos / tiempoTotalMinutos) * 100;

        if (porcentajeConsumido >= 80.0) {
            return "AMARILLO";
        }

        return "VERDE";
    }
}