package com.biometec.mesacontrol.repository;

import com.biometec.mesacontrol.entity.Equipo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio de acceso a datos para la entidad Equipo.
 *
 * Proporciona métodos de lectura para acceder a los datos de equipos.
 * Los equipos se cargan inicialmente desde una fuente de datos (Excel)
 * y no se modifican mediante operaciones de escritura en esta versión.
 *
 * @author Biometec
 * @version 1.0
 */
@Repository
public interface EquipoRepository extends JpaRepository<Equipo, Long> {

    /**
     * Obtiene una lista de todos los equipos.
     *
     * @return lista de todos los equipos
     */
    List<Equipo> findAll();

    /**
     * Busca un equipo por su identificador único.
     *
     * @param id el ID del equipo
     * @return un Optional que contiene el equipo si existe
     */
    Optional<Equipo> findById(Long id);

    /**
     * Obtiene todos los equipos pertenecientes a un cliente específico.
     * Útil para poblar combos anidados de equipos cuando se selecciona un cliente.
     *
     * @param clienteId el ID del cliente propietario de los equipos
     * @return lista de equipos del cliente especificado
     */
    List<Equipo> findByClienteId(Long clienteId);

    /**
     * Busca un equipo por su número de serie.
     *
     * @param numeroSerie el número de serie del equipo
     * @return un Optional que contiene el equipo si existe
     */
    Optional<Equipo> findByNumeroSerie(String numeroSerie);

    /**
     * Obtiene todos los equipos de una marca específica.
     *
     * @param marca la marca del equipo
     * @return lista de equipos de la marca especificada
     */
    List<Equipo> findByMarca(String marca);

    /**
     * Busca equipos cuyo nombre contenga el texto especificado.
     * Útil para búsquedas parciales en la interfaz de usuario.
     *
     * @param nombreParcial texto parcial del nombre del equipo
     * @return lista de equipos que coinciden con el criterio
     */
    @Query("SELECT e FROM Equipo e WHERE LOWER(e.nombreEquipo) LIKE LOWER(CONCAT('%', :nombre, '%'))")
    List<Equipo> findByNombreEquipoContainingIgnoreCase(@Param("nombre") String nombreParcial);

    /**
     * Cuenta la cantidad de equipos pertenecientes a un cliente específico.
     *
     * @param clienteId el ID del cliente
     * @return cantidad de equipos del cliente
     */
    Integer countByClienteId(Long clienteId);
}
