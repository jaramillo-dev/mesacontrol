package com.biometec.mesacontrol.dto;

import com.biometec.mesacontrol.entity.Rol;

/**
 * DTO de lectura utilizado para exponer información de usuarios a las vistas.
 *
 * <p>
 * Contiene únicamente los datos necesarios para consulta y listado,
 * evitando exponer información sensible como contraseñas u otros
 * atributos internos de la entidad.
 * </p>
 *
 * <p>
 * Incluye métodos auxiliares para simplificar el renderizado de las
 * plantillas Thymeleaf.
 * </p>
 *
 * @author Juan Jaramillo
 * @version 1.0
 */
public record UsuarioResponseDTO(
        Long id,
        String nombre,
        String email,
        Rol rol,
        Boolean activo
) {
    /**
     * Indica si el usuario se encuentra activo dentro del sistema.
     *
     * @return {@code true} si el usuario está habilitado; en otro caso {@code false}
     */
    public boolean estaActivo() {
        return Boolean.TRUE.equals(activo);
    }

    /**
     * Obtiene una representación legible del rol para su visualización
     * en las vistas.
     *
     * @return nombre del rol o un marcador vacío cuando no existe información
     */
    public String rolFormateado() {
        return rol != null ? rol.name() : "—";
    }
}