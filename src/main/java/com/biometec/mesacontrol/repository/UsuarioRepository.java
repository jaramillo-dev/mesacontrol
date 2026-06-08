package com.biometec.mesacontrol.repository;

import com.biometec.mesacontrol.entity.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario,Long> {
    Optional<Usuario> findByEmail(String email);
    Page<Usuario> findAll(Pageable pageable);
    Page<Usuario> findByActivoTrue(Pageable pageable);
    Page<Usuario> findByActivoFalse(Pageable pageable);
}
