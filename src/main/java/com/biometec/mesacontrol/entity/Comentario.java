package com.biometec.mesacontrol.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entidad que representa un comentario en el historial de un Ticket.
 * <p>
 * Regla de negocio crítica: Los comentarios son estrictamente inmutables.
 * Una vez creados, no pueden ser modificados ni eliminados por ningún rol.
 * </p>
 *
 * @author Juan Jaramillo
 * @version 1.0
 */
@Entity
@Table(name = "comentario")
public class Comentario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false, foreignKey = @ForeignKey(name = "fk_comentario_ticket"))
    private Ticket ticket;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "autor_id", nullable = false, foreignKey = @ForeignKey(name = "fk_comentario_autor"))
    private Usuario autor;

    @Column(nullable = false, columnDefinition = "TEXT", name = "comentario")
    private String texto;

    @Column(nullable = false, name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion;

    /**
     * Constructor vacío requerido por JPA.
     */
    public Comentario() {}

    /**
     * Constructor defensivo para garantizar la inmutabilidad y validación al nacer.
     */
    public Comentario(Ticket ticket, Usuario autor, String texto) {
        if (ticket == null || autor == null || texto == null || texto.trim().isEmpty()) {
            throw new IllegalArgumentException("Datos insuficientes para crear un comentario");
        }
        this.ticket = ticket;
        this.autor = autor;
        this.texto = texto;
        this.fechaCreacion = LocalDateTime.now();
    }

    // ==========================================
    // SOLO GETTERS (Sin Setters para garantizar inmutabilidad)
    // ==========================================
    public Long getId() { return id; }
    public Ticket getTicket() { return ticket; }
    public Usuario getAutor() { return autor; }
    public String getTexto() { return texto; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
}