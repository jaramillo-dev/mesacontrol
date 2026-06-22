package com.biometec.mesacontrol.repository;

import com.biometec.mesacontrol.entity.Rol;
import com.biometec.mesacontrol.entity.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario,Long> {

    Optional<Usuario> findByEmail(String email);

    Page<Usuario> findAll(Pageable pageable);

    Page<Usuario> findByActivoTrue(Pageable pageable);

    Page<Usuario> findByActivoFalse(Pageable pageable);

    /**
     * Obtiene todos los usuarios activos que pertenecen a un rol específico.
     * Útil para los combos dinámicos de asignación de tickets.
     *
     * @param rol el rol a buscar (VENTAS o TECNICO)
     * @param activo true para traer solo usuarios activos
     * @return lista de usuarios disponibles
     */
    List<Usuario> findByRolAndActivo(Rol rol, Boolean activo);
}
