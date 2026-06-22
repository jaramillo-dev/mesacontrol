package com.biometec.mesacontrol.entity;

import jakarta.persistence.*;
import java.util.Objects;

/**
 * Entidad que representa una unidad médica u hospital.
 *
 * Los datos de clientes se cargan inicialmente desde una fuente de datos (Excel)
 * y no se administran mediante un CRUD en esta versión de la aplicación.
 *
 * @author Biometec
 * @version 1.0
 */
@Entity
@Table(name = "cliente")
public class Cliente {

    /**
     * Identificador único del cliente.
     * Generado automáticamente por la base de datos.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Nombre de la unidad médica u hospital.
     * Requerido y no puede estar vacío.
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String nombre;

    /**
     * Ubicación geográfica del cliente.
     * Incluye dirección completa del establecimiento médico.
     * Requerido y no puede estar vacío.
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String ubicacion;

    // ========================
    // Constructores
    // ========================

    /**
     * Constructor vacío requerido por JPA.
     */
    public Cliente() {
    }

    /**
     * Constructor con parámetros principales.
     *
     * @param nombre el nombre del cliente
     * @param ubicacion la ubicación del cliente
     * @throws IllegalArgumentException si nombre o ubicacion son nulos/vacíos
     */
    public Cliente(String nombre, String ubicacion) {
        this.setNombre(nombre);
        this.setUbicacion(ubicacion);
    }

    // ========================
    // Getters y Setters
    // ========================

    /**
     * Obtiene el identificador único del cliente.
     *
     * @return el ID del cliente
     */
    public Long getId() {
        return id;
    }

    /**
     * Establece el identificador único del cliente.
     *
     * @param id el ID del cliente
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Obtiene el nombre del cliente.
     *
     * @return el nombre del cliente
     */
    public String getNombre() {
        return nombre;
    }

    /**
     * Establece el nombre del cliente.
     * Valida que no sea nulo o vacío.
     *
     * @param nombre el nombre del cliente
     * @throws IllegalArgumentException si el nombre está vacío o es nulo
     */
    public void setNombre(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del cliente no puede estar vacío");
        }
        this.nombre = nombre.trim();
    }

    /**
     * Obtiene la ubicación del cliente.
     *
     * @return la ubicación del cliente
     */
    public String getUbicacion() {
        return ubicacion;
    }

    /**
     * Establece la ubicación del cliente.
     * Valida que no sea nula o vacía.
     *
     * @param ubicacion la ubicación del cliente
     * @throws IllegalArgumentException si la ubicación está vacía o es nula
     */
    public void setUbicacion(String ubicacion) {
        if (ubicacion == null || ubicacion.trim().isEmpty()) {
            throw new IllegalArgumentException("La ubicación del cliente no puede estar vacía");
        }
        this.ubicacion = ubicacion.trim();
    }

    // ========================
    // equals() y hashCode()
    // ========================

    /**
     * Compara dos clientes por su identificador único.
     * Para objetos transientes (sin ID), usa el nombre como identificador.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Cliente cliente = (Cliente) o;

        if (this.id != null && cliente.id != null) {
            return Objects.equals(this.id, cliente.id);
        }

        return Objects.equals(this.nombre, cliente.nombre);
    }

    /**
     * Genera el código hash del cliente basado en su ID o nombre.
     */
    @Override
    public int hashCode() {
        if (this.id != null) {
            return Objects.hash(id);
        }
        return Objects.hash(nombre);
    }

    // ========================
    // toString()
    // ========================

    /**
     * Representación en string del cliente.
     */
    @Override
    public String toString() {
        return "Cliente{" +
                "id=" + id +
                ", nombre='" + nombre + '\'' +
                ", ubicacion='" + ubicacion + '\'' +
                '}';
    }
}
