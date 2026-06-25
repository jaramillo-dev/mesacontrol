package com.biometec.mesacontrol.repository;

import com.biometec.mesacontrol.dto.DashboardStatsDTO;
import com.biometec.mesacontrol.entity.EstadoTicket;
import com.biometec.mesacontrol.entity.PrioridadTicket;
import com.biometec.mesacontrol.entity.Ticket;
import com.biometec.mesacontrol.entity.TipoTicket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio de acceso a datos para la entidad Ticket.
 *
 * Proporciona métodos de lectura, escritura y consultas especializadas para gestionar tickets de servicio y venta.
 *
 * @author Juan Jaramillo
 * @version 1.0
 */
@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    /**
     * Busca un ticket por su folio único.
     *
     * @param folio el folio del ticket
     * @return un Optional que contiene el ticket si existe
     */
    Optional<Ticket> findByFolio(String folio);

    /**
     * Obtiene todos los tickets de un cliente específico.
     *
     * @param clienteId el ID del cliente
     * @return lista de tickets del cliente
     */
    List<Ticket> findByClienteId(Long clienteId);

    /**
     * Obtiene todos los tickets de un cliente con paginación.
     *
     * @param clienteId el ID del cliente
     * @param pageable información de paginación
     * @return página de tickets del cliente
     */
    Page<Ticket> findByClienteId(Long clienteId, Pageable pageable);

    /**
     * Obtiene todos los tickets asociados a un equipo específico.
     *
     * @param equipoId el ID del equipo
     * @return lista de tickets del equipo
     */
    List<Ticket> findByEquipoId(Long equipoId);

    /**
     * Obtiene todos los tickets asignados a un responsable específico.
     *
     * @param responsableId el ID del usuario responsable
     * @return lista de tickets responsabilidad del usuario
     */
    List<Ticket> findByResponsableId(Long responsableId);

    /**
     * Obtiene todos los tickets asignados a un responsable con paginación.
     *
     * @param responsableId el ID del usuario responsable
     * @param pageable información de paginación
     * @return página de tickets del responsable
     */
    Page<Ticket> findByResponsableId(Long responsableId, Pageable pageable);

    /**
     * Obtiene todos los tickets con un estado específico.
     *
     * @param estado el estado del ticket
     * @return lista de tickets en ese estado
     */
    List<Ticket> findByEstado(EstadoTicket estado);

    /**
     * Obtiene todos los tickets con un estado específico usando paginación.
     *
     * @param estado el estado del ticket
     * @param pageable información de paginación
     * @return página de tickets en ese estado
     */
    Page<Ticket> findByEstado(EstadoTicket estado, Pageable pageable);

    /**
     * Obtiene todos los tickets con un estado específico y un responsable especifico usando paginación.
     *
     * @param tipo el tipo del ticket
     * @param responsableId el id del responsable del ticket
     * @param pageable información de paginación
     * @return página de tickets de ese tipo y de ese responsable
     */
    Page<Ticket> findByTipoAndResponsableId(TipoTicket tipo, Long responsableId, Pageable pageable);

    /**
     * Obtiene todos los tickets con una prioridad específica.
     *
     * @param prioridad la prioridad del ticket
     * @return lista de tickets con esa prioridad
     */
    List<Ticket> findByPrioridad(PrioridadTicket prioridad);

    /**
     * Obtiene todos los tickets de un tipo específico.
     *
     * @param tipo el tipo del ticket
     * @return lista de tickets de ese tipo
     */
    List<Ticket> findByTipo(TipoTicket tipo);

    /**
     * Obtiene tickets abiertos (ABIERTO o EN_PROCESO) con paginación.
     *
     * @param pageable información de paginación
     * @return página de tickets abiertos
     */
    @Query("SELECT t FROM Ticket t WHERE t.estado IN :estados ORDER BY t.prioridad ASC, t.fechaCreacion ASC")
    Page<Ticket> findTicketsAbiertos(@Param("estados") List<EstadoTicket> estados, Pageable pageable);

    /**
     * Obtiene tickets vencidos (fecha actual > fecha_vencimiento_sla y estado!= CERRADO).
     * @return lista de tickets vencidos
     */
    @Query("SELECT t FROM Ticket t WHERE t.fechaVencimientoSla < CURRENT_TIMESTAMP AND t.estado <> :cerrado ORDER BY t.fechaVencimientoSla ASC")
    List<Ticket> findTicketsVencidos(@Param("cerrado") EstadoTicket cerrado);

    /**
     * Obtiene tickets con SLA próximo a vencer (dentro de X horas).
     *
     * @param fechaLimite número de horas para verificar próximo vencimiento
     * @return lista de tickets próximos a vencer
     */
    @Query("SELECT t FROM Ticket t WHERE t.fechaVencimientoSla BETWEEN CURRENT_TIMESTAMP AND :fechaLimite AND t.estado <> :cerrado ORDER BY t.fechaVencimientoSla ASC")
    List<Ticket> findTicketsProximosAVencer(
            @Param("fechaLimite") LocalDateTime fechaLimite,
            @Param("cerrado") EstadoTicket cerrado
    );

    /**
     * Obtiene tickets cerrados en un rango de fechas.
     *
     * @param desde fecha de inicio (inclusive)
     * @param hasta fecha de fin (inclusive)
     * @return lista de tickets cerrados en el rango
     */
    @Query("SELECT t FROM Ticket t WHERE t.estado = :estado AND t.fechaCierre BETWEEN :desde AND :hasta ORDER BY t.fechaCierre DESC")
    List<Ticket> findTicketsCerradosEnRango(
            @Param("estado") EstadoTicket estado,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta
    );

    /**
     * Cuenta el número de tickets por estado.
     *
     * @param estado el estado del ticket
     * @return cantidad de tickets en ese estado
     */
    Long countByEstado(EstadoTicket estado);

    /**
     * Recupera todas las estadísticas requeridas por el Dashboard en una sola consulta.
     * <p>
     * Si :responsableId es NULL, se calculan las métricas globales (esquema para ADMIN).
     * Si se provee el ID, se calcula estrictamente la carga de trabajo de ese usuario.
     * </p>
     */
    @Query("""
        SELECT new com.biometec.mesacontrol.dto.DashboardStatsDTO(
            COUNT(CASE WHEN t.estado = :abierto THEN 1 END),
            COUNT(CASE WHEN t.estado = :enProceso THEN 1 END),
            COUNT(CASE WHEN t.estado = :cerrado THEN 1 END),
            COUNT(CASE WHEN t.fechaVencimientoSla < CURRENT_TIMESTAMP AND t.estado <> :cerrado THEN 1 END)
        )
        FROM Ticket t
        WHERE (:responsableId IS NULL OR t.responsable.id = :responsableId)
    """)
    DashboardStatsDTO obtenerEstadisticasDashboard(
            @Param("responsableId") Long responsableId,
            @Param("abierto") EstadoTicket abierto,
            @Param("enProceso") EstadoTicket enProceso,
            @Param("cerrado") EstadoTicket cerrado
    );


    /**
     * Cuenta el número de tickets de un cliente.
     *
     * @param clienteId el ID del cliente
     * @return cantidad de tickets del cliente
     */
    Long countByClienteId(Long clienteId);

    /**
     * Cuenta el número de tickets asignados a un responsable.
     *
     * @param responsableId el ID del usuario responsable
     * @return cantidad de tickets del responsable
     */
    Long countByResponsableId(Long responsableId);

    /**
     * Busca tickets donde el folio contenga el texto especificado.
     *
     * @param folioParcial texto parcial del folio
     * @param pageable información de paginación
     * @return página de tickets que coinciden
     */
    @Query("SELECT t FROM Ticket t WHERE UPPER(t.folio) LIKE UPPER(CONCAT('%', :folio, '%')) ORDER BY t.fechaCreacion DESC")
    Page<Ticket> findByFolioContaining(@Param("folio") String folioParcial, Pageable pageable);

    /**
     * Obtiene una página de tickets activos pertenecientes a un área específica
     * y asignados a un usuario responsable en particular.
     * <p>
     * Se ordena por la fecha de creación de forma descendente (los más recientes primero).
     * </p>
     *
     * @param tipo el tipo de ticket que corresponde al área (VENTA o SERVICIO)
     * @param responsableId el identificador único del usuario asignado
     * @param pageable información de paginación de Spring Data
     * @return página con los tickets que cumplen estrictamente con ambos criterios
     */
    @Query("SELECT t FROM Ticket t WHERE t.tipo = :tipo AND t.responsable.id = :responsableId AND t.estado IN :estados ORDER BY t.fechaCreacion DESC")
    Page<Ticket> findByTipoAndResponsableAndEstados(
            @Param("tipo") TipoTicket tipo,
            @Param("responsableId") Long responsableId,
            @Param("estados") List<EstadoTicket> estados,
            Pageable pageable
    );

    /**
     * Busca tickets cuyo folio coincida parcialmente (Ignore Case) de manera global.
     * Esquema utilizado exclusivamente por el rol ADMIN.
     *
     * @param queryFolio cadena formateada con comodines (ej. %TK-2026%)
     * @param pageable   configuración de paginación de Spring Data
     * @return página de tickets que coinciden con el criterio
     */
    @Query("SELECT t FROM Ticket t WHERE UPPER(t.folio) LIKE UPPER(:queryFolio) ORDER BY t.prioridad ASC, t.fechaCreacion ASC")
    Page<Ticket> findByFolioContainingIgnoreCase(@Param("queryFolio") String queryFolio, Pageable pageable);

    /**
     * Busca tickets por coincidencia parcial de folio, restringido por área (Tipo) y responsable operativo.
     * Esquema utilizado por los roles VENTAS y TECNICO para buscar dentro de sus asignaciones.
     *
     * @param queryFolio    cadena formateada con comodines (ej. %TK-2026%)
     * @param tipo          área funcional (VENTA o SERVICIO)
     * @param responsableId identificador único del usuario asignado
     * @param pageable      configuración de paginación de Spring Data
     * @return página de tickets que cumplen estrictamente con los tres filtros
     */
    @Query("""
        SELECT t FROM Ticket t 
        WHERE UPPER(t.folio) LIKE UPPER(:queryFolio) 
          AND t.tipo = :tipo 
          AND t.responsable.id = :responsableId 
        ORDER BY t.prioridad ASC, t.fechaCreacion ASC
    """)
    Page<Ticket> findByFolioAndTipoAndResponsable(
            @Param("queryFolio") String queryFolio,
            @Param("tipo") TipoTicket tipo,
            @Param("responsableId") Long responsableId,
            Pageable pageable
    );

    /**
     * Busca tickets filtrados por Estado de Ciclo de Vida, adaptándose dinámicamente al rol.
     * <p>
     * Si los parámetros :responsableId o :tipo se envían como NULL, la consulta omite
     * el filtro de forma automática (Comportamiento irrestricto para ADMIN).
     * </p>
     */
    @Query("""
    SELECT t FROM Ticket t 
    WHERE t.estado = :estado 
      AND (:tipo IS NULL OR t.tipo = :tipo) 
      AND (:responsableId IS NULL OR t.responsable.id = :responsableId) 
    ORDER BY t.fechaCreacion DESC
    """)
    Page<Ticket> findPorEstadoRolYResponsable(
            @Param("estado") EstadoTicket estado,
            @Param("responsableId") Long responsableId,
            @Param("tipo") TipoTicket tipo,
            Pageable pageable
    );

    /**
     * Recupera los tickets activos cuyo Service Level Agreement (SLA) ha expirado.
     * <p>
     * Un ticket se considera vencido si la fecha límite es menor al tiempo actual
     * del sistema y el estado NO es CERRADO. Se adapta al rol del usuario logueado.
     * </p>
     */
    @Query("""
        SELECT t FROM Ticket t 
        WHERE t.fechaVencimientoSla < CURRENT_TIMESTAMP 
          AND t.estado <> com.biometec.mesacontrol.entity.EstadoTicket.CERRADO 
          AND (:tipo IS NULL OR t.tipo = :tipo) 
          AND (:responsableId IS NULL OR t.responsable.id = :responsableId) 
        ORDER BY t.fechaVencimientoSla ASC
    """)
    Page<Ticket> findVencidosPorFiltro(
            @Param("responsableId") Long responsableId,
            @Param("tipo") TipoTicket tipo,
            Pageable pageable
    );

    /**
     * Consulta Maestra Dinámica: Combina Búsqueda por Folio, Estado y Paginación simultáneamente.
     */
    @Query("""
    SELECT t FROM Ticket t 
    WHERE (CAST(:folio AS string) IS NULL OR UPPER(t.folio) LIKE :folio)
    AND (:estado IS NULL OR t.estado = :estado)
    AND (:tipo IS NULL OR t.tipo = :tipo)
    AND (:responsableId IS NULL OR t.responsable.id = :responsableId)
    AND (:esVencido = false OR (t.fechaVencimientoSla < CURRENT_TIMESTAMP AND t.estado <> com.biometec.mesacontrol.entity.EstadoTicket.CERRADO))
    ORDER BY t.fechaCreacion DESC
    """)
    Page<Ticket> findTicketsCombinados(
            @Param("folio") String folio,
            @Param("estado") EstadoTicket estado,
            @Param("tipo") TipoTicket tipo,
            @Param("responsableId") Long responsableId,
            @Param("esVencido") boolean esVencido,
            Pageable pageable
    );
}