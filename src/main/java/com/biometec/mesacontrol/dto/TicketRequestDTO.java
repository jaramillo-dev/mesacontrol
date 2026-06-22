package com.biometec.mesacontrol.dto;

import com.biometec.mesacontrol.entity.PrioridadTicket;
import com.biometec.mesacontrol.entity.TipoTicket;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO utilizado para la captura y actualización de tickets.
 *
 * <p>
 * Desacopla los formularios de la capa de persistencia y centraliza
 * las reglas de validación aplicables durante el ciclo de vida del ticket.
 * </p>
 *
 * <p>
 * Utiliza grupos de validación para diferenciar requisitos de creación
 * y modificación.
 * </p>
 * @author Juan Jaramillo
 * @version 1.0
 */
public class TicketRequestDTO {

    @NotNull(message = "El tipo de ticket es requerido", groups = OnCreate.class)
    private TipoTicket tipo;

    @NotNull(message = "La prioridad del ticket es requerida", groups = {OnCreate.class, OnUpdate.class})
    private PrioridadTicket prioridad;

    @NotBlank(message = "La descripción inicial es requerida", groups = OnCreate.class)
    @Size(min = 10, max = 2000, message = "La descripción debe tener entre 10 y 2000 caracteres", groups = OnCreate.class)
    private String descripcionInicial;

    @NotNull(message = "El cliente es requerido", groups = OnCreate.class)
    private Long clienteId;

    @NotNull(message = "El equipo es requerido", groups = OnCreate.class)
    private Long equipoId;

    @NotNull(message = "El usuario responsable es mandatorio al momento de crear el ticket", groups = OnCreate.class)
    private Long responsableId;

    @Size(max = 50, message = "El folio de cotización no puede exceder 50 caracteres", groups = OnUpdate.class)
    private String folioCotizacion;

    @Size(max = 50, message = "El folio de orden de servicio no puede exceder 50 caracteres", groups = OnUpdate.class)
    private String folioOrdenServicio;

    public TicketRequestDTO() {
    }

    public TipoTicket getTipo() {
        return tipo;
    }

    public void setTipo(TipoTicket tipo) {
        this.tipo = tipo;
    }

    public PrioridadTicket getPrioridad() {
        return prioridad;
    }

    public void setPrioridad(PrioridadTicket prioridad) {
        this.prioridad = prioridad;
    }

    public String getDescripcionInicial() {
        return descripcionInicial;
    }

    public void setDescripcionInicial(String descripcionInicial) {
        this.descripcionInicial = descripcionInicial;
    }

    public Long getClienteId() {
        return clienteId;
    }

    public void setClienteId(Long clienteId) {
        this.clienteId = clienteId;
    }

    public Long getEquipoId() {
        return equipoId;
    }

    public void setEquipoId(Long equipoId) {
        this.equipoId = equipoId;
    }

    public Long getResponsableId() {
        return responsableId;
    }

    public void setResponsableId(Long responsableId) {
        this.responsableId = responsableId;
    }

    public String getFolioCotizacion() {
        return folioCotizacion;
    }

    public void setFolioCotizacion(String folioCotizacion) {
        this.folioCotizacion = folioCotizacion;
    }

    public String getFolioOrdenServicio() {
        return folioOrdenServicio;
    }

    public void setFolioOrdenServicio(String folioOrdenServicio) {
        this.folioOrdenServicio = folioOrdenServicio;
    }
}