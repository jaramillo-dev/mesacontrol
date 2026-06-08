package com.biometec.mesacontrol.dto;

import com.biometec.mesacontrol.entity.Rol;

public record UsuarioResponseDTO(
        Long id,
        String nombre,
        String email,
        Rol rol,
        Boolean activo
) {
    // Helpers para las plantillas Thymeleaf
    public boolean estaActivo() {
        return Boolean.TRUE.equals(activo);
    }

    public String rolFormateado() {
        return rol != null ? rol.name() : "—";
    }
}