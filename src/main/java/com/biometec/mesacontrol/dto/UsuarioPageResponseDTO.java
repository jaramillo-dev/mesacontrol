package com.biometec.mesacontrol.dto;

import java.util.List;

public record UsuarioPageResponseDTO(
        List<UsuarioResponseDTO> contenido,
        int paginaActual,
        int totalPaginas,
        long totalElementos,
        int tamanioPagina
) {
    public boolean hayAnterior() { return paginaActual > 0; }
    public boolean haySiguiente() { return paginaActual < (totalPaginas - 1); }
}
