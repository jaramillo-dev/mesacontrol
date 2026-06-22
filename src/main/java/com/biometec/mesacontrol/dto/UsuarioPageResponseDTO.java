package com.biometec.mesacontrol.dto;

import java.util.List;

/**
 * DTO contenedor utilizado para transportar resultados paginados
 * de usuarios hacia la capa de presentación.
 *
 * <p>
 * Agrupa tanto los registros obtenidos como la información necesaria
 * para construir controles de paginación en la interfaz.
 * </p>
 *
 * @author Juan Jaramillo
 * @version 1.0
 */
public record UsuarioPageResponseDTO(
        List<UsuarioResponseDTO> contenido,
        int paginaActual,
        int totalPaginas,
        long totalElementos,
        int tamanioPagina
) {
    /**
     * Indica si existe una página anterior disponible.
     *
     * @return {@code true} cuando la navegación puede retroceder una página
     */
    public boolean hayAnterior() { return paginaActual > 0; }

    /**
     * Indica si existe una página posterior disponible.
     *
     * @return {@code true} cuando la navegación puede avanzar una página
     */
    public boolean haySiguiente() { return paginaActual < (totalPaginas - 1); }
}
