package com.biometec.mesacontrol.mapper;

import com.biometec.mesacontrol.dto.UsuarioPageResponseDTO;
import com.biometec.mesacontrol.dto.UsuarioRequestDTO;
import com.biometec.mesacontrol.dto.UsuarioResponseDTO;
import com.biometec.mesacontrol.entity.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UsuarioMapper {

    // ========================
    // Entidad → ResponseDTO
    // ========================

    public UsuarioResponseDTO toResponseDTO(Usuario usuario) {
        return new UsuarioResponseDTO(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getEmail(),
                usuario.getRol(),
                usuario.getActivo()
        );
    }

    // ========================
    // Page<Entidad> → PageResponseDTO
    // ========================

    public UsuarioPageResponseDTO toPageResponseDTO(Page<Usuario> page) {
        List<UsuarioResponseDTO> contenido = page.getContent()
                .stream()
                .map(this::toResponseDTO)
                .toList();

        return new UsuarioPageResponseDTO(
                contenido,
                page.getNumber(),
                page.getTotalPages(),
                page.getTotalElements(),
                page.getSize()
        );
    }

    // ========================
    // RequestDTO → Entidad (creación)
    // ========================

    public Usuario toEntity(UsuarioRequestDTO dto) {
        // Usamos el constructor defensivo de Usuario que ya valida campos
        return new Usuario(
                dto.getNombre(),
                dto.getEmail(),
                dto.getPassword(),
                dto.getRol()
        );
    }

    // ========================
    // RequestDTO → Entidad existente (edición)
    // Solo actualiza los campos que llegan; la contraseña se omite aquí
    // porque el servicio decide si cifrarla o ignorarla.
    // ========================

    public void updateEntityFromDTO(UsuarioRequestDTO dto, Usuario usuario) {
        if (dto.getNombre() != null && !dto.getNombre().isBlank()) {
            usuario.setNombre(dto.getNombre());
        }
        if (dto.getEmail() != null && !dto.getEmail().isBlank()) {
            usuario.setEmail(dto.getEmail());
        }
        if (dto.getRol() != null) {
            usuario.setRol(dto.getRol());
        }
        // La contraseña se pasa cruda; el servicio la cifrará si no es blank
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            usuario.setPassword(dto.getPassword());
        }
    }

    // ========================
    // ResponseDTO → RequestDTO (edición)
    // ========================

    public UsuarioRequestDTO toRequestDTOFromResponse(UsuarioResponseDTO responseDTO) {
        UsuarioRequestDTO dto = new UsuarioRequestDTO();
        dto.setNombre(responseDTO.nombre());
        dto.setEmail(responseDTO.email());
        dto.setRol(responseDTO.rol());
        return dto;
    }
}
