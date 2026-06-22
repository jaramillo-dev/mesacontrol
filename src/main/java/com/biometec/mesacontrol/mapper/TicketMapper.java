package com.biometec.mesacontrol.mapper;

import com.biometec.mesacontrol.dto.TicketResponseDTO;
import com.biometec.mesacontrol.entity.Ticket;
import org.springframework.stereotype.Component;

/**
 * Mapper componente encargado de transformar objetos de dominio Ticket
 * a estructuras DTO de salida orientadas a la vista.
 *
 * @author Juan Jaramillo
 * @version 1.0
 */
@Component
public class TicketMapper {

    /**
     * Transforma una entidad de persistencia Ticket a un TicketResponseDTO amigable para la UI.
     * Evalúa de forma segura las referencias Lazy para evitar NullPointerException.
     *
     * @param ticket entidad origen de la base de datos
     * @return DTO de respuesta estructurado o null si la entidad es nula
     */
    public TicketResponseDTO toResponseDTO(Ticket ticket) {
        if (ticket == null) {
            return null;
        }

        TicketResponseDTO dto = new TicketResponseDTO();
        dto.setId(ticket.getId());
        dto.setFolio(ticket.getFolio());
        dto.setTipo(ticket.getTipo());
        dto.setPrioridad(ticket.getPrioridad());
        dto.setEstado(ticket.getEstado());
        dto.setDescripcionInicial(ticket.getDescripcionInicial());
        dto.setFechaCreacion(ticket.getFechaCreacion());
        dto.setFechaVencimientoSla(ticket.getFechaVencimientoSla());
        dto.setFechaCierre(ticket.getFechaCierre());
        dto.setFolioCotizacion(ticket.getFolioCotizacion());
        dto.setFolioOrdenServicio(ticket.getFolioOrdenServicio());

        // Mapeo seguro de relaciones relativas al Cliente
        if (ticket.getCliente() != null) {
            dto.setClienteId(ticket.getCliente().getId());
            dto.setClienteNombre(ticket.getCliente().getNombre());
        }

        // Mapeo seguro de relaciones relativas al Equipo médico
        if (ticket.getEquipo() != null) {
            dto.setEquipoId(ticket.getEquipo().getId());
            dto.setEquipoNombre(ticket.getEquipo().getNombreEquipo());
            dto.setEquipoNumeroSerie(ticket.getEquipo().getNumeroSerie());
        }

        // Mapeo seguro de la relación opcional con el Usuario Responsable
        if (ticket.getResponsable() != null) {
            dto.setResponsableId(ticket.getResponsable().getId());
            dto.setResponsableNombre(ticket.getResponsable().getNombre());
        }

        return dto;
    }
}
