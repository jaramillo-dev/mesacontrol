package com.biometec.mesacontrol.repository;

import com.biometec.mesacontrol.entity.Comentario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ComentarioRepository extends JpaRepository<Comentario, Long> {

    /**
     * Obtiene el historial cronológico de un ticket.
     *
     * @param ticketId ID del ticket
     * @return lista de comentarios ordenados del más antiguo al más reciente
     */
    List<Comentario> findByTicketIdOrderByFechaCreacionAsc(Long ticketId);
}
