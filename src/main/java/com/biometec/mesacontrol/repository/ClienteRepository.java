package com.biometec.mesacontrol.repository;

import com.biometec.mesacontrol.entity.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio de acceso a datos para la entidad Cliente.
 *
 * Proporciona métodos de lectura para acceder a los datos de clientes.
 * Los clientes se cargan inicialmente desde una fuente de datos (Excel)
 * y no se modifican mediante operaciones de escritura en esta versión.
 *
 * @author Juan Jaramillo
 * @version 1.0
 */
@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    /**
     * Obtiene una lista de todos los clientes.
     * Útil para poblar combos selectores en la interfaz de usuario.
     *
     * @return lista ordenada de todos los clientes
     */
    List<Cliente> findAll();

    /**
     * Busca un cliente por su identificador único.
     *
     * @param id el ID del cliente
     * @return un Optional que contiene el cliente si existe
     */
    Optional<Cliente> findById(Long id);

    /**
     * Busca un cliente por su nombre exacto.
     *
     * @param nombre el nombre del cliente
     * @return un Optional que contiene el cliente si existe
     */
    Optional<Cliente> findByNombre(String nombre);

    /**
     * Busca clientes cuyo nombre contenga el texto especificado.
     * La búsqueda no distingue entre mayúsculas y minúsculas.
     *
     * @param nombre texto parcial del nombre del cliente
     * @return lista de clientes que coinciden con el criterio
     */
    @Query("SELECT c FROM Cliente c WHERE LOWER(c.nombre) LIKE LOWER(CONCAT('%', :nombre, '%'))")
    List<Cliente> findByNombreContainingIgnoreCase(String nombre);
}
