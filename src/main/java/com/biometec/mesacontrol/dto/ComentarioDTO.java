package com.biometec.mesacontrol.dto;

import java.time.LocalDateTime;

/**
 * DTO inmutable utilizado para representar comentarios dentro del
 * historial de seguimiento de un ticket.
 *
 * <p>
 * Expone únicamente la información requerida por la capa de presentación,
 * evitando dependencias directas con las entidades JPA.
 * </p>
 * @author Juan Jaramillo
 * @version 1.0
 */
public record ComentarioDTO(
        Long id,
        String texto,
        String autorNombre,
        LocalDateTime fechaCreacion
) {}