package com.biometec.mesacontrol.entity;

import jakarta.persistence.*;
import java.util.Objects;

/**
 * Entidad que representa un equipo médico perteneciente a un cliente.
 *
 * Los datos de equipos se cargan inicialmente desde una fuente de datos (Excel)
 * y no se administran mediante un CRUD en esta versión de la aplicación.
 *
 * @author Juan Jaramillo
 * @version 1.0
 */
@Entity
@Table(name = "equipo")
public class Equipo {

    /**
     * Identificador único del equipo.
     * Generado automáticamente por la base de datos.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Relación con el cliente propietario del equipo.
     * Establece una relación de muchos-a-uno (ManyToOne).
     * No puede ser nulo.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false, foreignKey = @ForeignKey(name = "fk_equipo_cliente"))
    private Cliente cliente;

    /**
     * Nombre descriptivo del equipo médico.
     * Ejemplo: "ANGIOGRAFO ARCO BIPLANAR", "MASTOGRAFO DIGITAL"
     * Requerido y no puede estar vacío.
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String nombreEquipo;

    /**
     * Marca fabricante del equipo.
     * Ejemplo: "SIEMENS", "PHILIPS", "HOLOGIC"
     * Requerido y no puede estar vacío.
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String marca;

    /**
     * Modelo específico del equipo.
     * Ejemplo: "ARTIS ZEE", "ALLURA CV20"
     * Requerido y no puede estar vacío.
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String modelo;

    /**
     * Número de serie del equipo.
     * Identificador único del fabricante.
     * Requerido y no puede estar vacío.
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String numeroSerie;

    // ========================
    // Constructores
    // ========================

    /**
     * Constructor vacío requerido por JPA.
     */
    public Equipo() {
    }

    /**
     * Constructor con parámetros principales.
     *
     * @param cliente el cliente propietario del equipo
     * @param nombreEquipo el nombre descriptivo del equipo
     * @param marca la marca del equipo
     * @param modelo el modelo del equipo
     * @param numeroSerie el número de serie del equipo
     * @throws IllegalArgumentException si algún parámetro es nulo o vacío
     */
    public Equipo(Cliente cliente, String nombreEquipo, String marca, String modelo, String numeroSerie) {
        this.setCliente(cliente);
        this.setNombreEquipo(nombreEquipo);
        this.setMarca(marca);
        this.setModelo(modelo);
        this.setNumeroSerie(numeroSerie);
    }

    // ========================
    // Getters y Setters
    // ========================

    /**
     * Obtiene el identificador único del equipo.
     *
     * @return el ID del equipo
     */
    public Long getId() {
        return id;
    }

    /**
     * Establece el identificador único del equipo.
     *
     * @param id el ID del equipo
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Obtiene el cliente propietario del equipo.
     *
     * @return el cliente del equipo
     */
    public Cliente getCliente() {
        return cliente;
    }

    /**
     * Establece el cliente propietario del equipo.
     * Valida que el cliente no sea nulo.
     *
     * @param cliente el cliente del equipo
     * @throws IllegalArgumentException si el cliente es nulo
     */
    public void setCliente(Cliente cliente) {
        if (cliente == null) {
            throw new IllegalArgumentException("El cliente del equipo no puede ser nulo");
        }
        this.cliente = cliente;
    }

    /**
     * Obtiene el nombre descriptivo del equipo.
     *
     * @return el nombre del equipo
     */
    public String getNombreEquipo() {
        return nombreEquipo;
    }

    /**
     * Establece el nombre descriptivo del equipo.
     * Valida que no sea nulo o vacío.
     *
     * @param nombreEquipo el nombre del equipo
     * @throws IllegalArgumentException si el nombre está vacío o es nulo
     */
    public void setNombreEquipo(String nombreEquipo) {
        if (nombreEquipo == null || nombreEquipo.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del equipo no puede estar vacío");
        }
        this.nombreEquipo = nombreEquipo.trim();
    }

    /**
     * Obtiene la marca del equipo.
     *
     * @return la marca del equipo
     */
    public String getMarca() {
        return marca;
    }

    /**
     * Establece la marca del equipo.
     * Valida que no sea nula o vacía.
     *
     * @param marca la marca del equipo
     * @throws IllegalArgumentException si la marca está vacía o es nula
     */
    public void setMarca(String marca) {
        if (marca == null || marca.trim().isEmpty()) {
            throw new IllegalArgumentException("La marca del equipo no puede estar vacía");
        }
        this.marca = marca.trim();
    }

    /**
     * Obtiene el modelo del equipo.
     *
     * @return el modelo del equipo
     */
    public String getModelo() {
        return modelo;
    }

    /**
     * Establece el modelo del equipo.
     * Valida que no sea nulo o vacío.
     *
     * @param modelo el modelo del equipo
     * @throws IllegalArgumentException si el modelo está vacío o es nulo
     */
    public void setModelo(String modelo) {
        if (modelo == null || modelo.trim().isEmpty()) {
            throw new IllegalArgumentException("El modelo del equipo no puede estar vacío");
        }
        this.modelo = modelo.trim();
    }

    /**
     * Obtiene el número de serie del equipo.
     *
     * @return el número de serie del equipo
     */
    public String getNumeroSerie() {
        return numeroSerie;
    }

    /**
     * Establece el número de serie del equipo.
     * Valida que no sea nulo o vacío.
     *
     * @param numeroSerie el número de serie del equipo
     * @throws IllegalArgumentException si el número de serie está vacío o es nulo
     */
    public void setNumeroSerie(String numeroSerie) {
        if (numeroSerie == null || numeroSerie.trim().isEmpty()) {
            throw new IllegalArgumentException("El número de serie del equipo no puede estar vacío");
        }
        this.numeroSerie = numeroSerie.trim();
    }

    // ========================
    // equals() y hashCode()
    // ========================

    /**
     * Compara dos equipos por su identificador único.
     * Para objetos transientes (sin ID), usa el número de serie como identificador.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Equipo equipo = (Equipo) o;

        if (this.id != null && equipo.id != null) {
            return Objects.equals(this.id, equipo.id);
        }

        return Objects.equals(this.numeroSerie, equipo.numeroSerie);
    }

    /**
     * Genera el código hash del equipo basado en su ID o número de serie.
     */
    @Override
    public int hashCode() {
        if (this.id != null) {
            return Objects.hash(id);
        }
        return Objects.hash(numeroSerie);
    }

    // ========================
    // toString()
    // ========================

    /**
     * Representación en string del equipo.
     */
    @Override
    public String toString() {
        return "Equipo{" +
                "id=" + id +
                ", cliente=" + (cliente != null ? cliente.getId() : null) +
                ", nombreEquipo='" + nombreEquipo + '\'' +
                ", marca='" + marca + '\'' +
                ", modelo='" + modelo + '\'' +
                ", numeroSerie='" + numeroSerie + '\'' +
                '}';
    }
}
