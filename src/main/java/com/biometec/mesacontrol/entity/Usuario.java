package com.biometec.mesacontrol.entity;

import jakarta.persistence.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

@Entity
@Table(name = "usuario", uniqueConstraints = {
        @UniqueConstraint(columnNames = "email", name = "uk_usuario_email")
})
public class Usuario implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String nombre;

    @Column(nullable = false, length = 255, unique = true)
    private String email;

    @Column(nullable = false, length = 255)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Rol rol;

    @Column(nullable = false)
    private Boolean activo = true;

    // ========================
    // Constructores
    // ========================

    /**
     * Constructor vacío requerido por JPA
     */
    protected Usuario() {
    }

    /**
     * Constructor defensivo con validaciones
     */
    public Usuario(String nombre, String email, String password, Rol rol) {
        this.setNombre(nombre);
        this.setEmail(email);
        this.setPassword(password);
        this.setRol(rol);
        this.activo = true;
    }

    // ========================
    // Getters y Setters
    // ========================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre no puede estar vacío");
        }
        this.nombre = nombre.trim();
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("El email no puede estar vacío");
        }
        String trimmedEmail = email.trim().toLowerCase();
        if (!trimmedEmail.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new IllegalArgumentException("El email no tiene un formato válido");
        }
        this.email = trimmedEmail;
    }

    @Override
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("La contraseña no puede estar vacía");
        }
        this.password = password;
    }

    public Rol getRol() {
        return rol;
    }

    public void setRol(Rol rol) {
        if (rol == null) {
            throw new IllegalArgumentException("El rol no puede ser nulo");
        }
        this.rol = rol;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo != null ? activo : true;
    }

    // ========================
    // Implementación de UserDetails
    // ========================

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // El usuario solo tiene un rol, retornamos una colección con ese único rol
        return Collections.singleton(new SimpleGrantedAuthority("ROLE_" + rol.name()));
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return activo;
    }

    // ========================
    // equals() y hashCode()
    // ========================

    /**
     * Implementación defensiva de equals y hashCode.
     * Usamos el ID como identificador único en la BD.
     * Para objetos transientes (sin ID), usamos el email que también es único.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Usuario usuario = (Usuario) o;

        // Si ambos tienen ID, comparamos por ID
        if (this.id != null && usuario.id != null) {
            return Objects.equals(this.id, usuario.id);
        }

        // Si alguno es transiente, comparamos por email (que también es único)
        return Objects.equals(this.email, usuario.email);
    }

    @Override
    public int hashCode() {
        // Si el objeto tiene ID, usamos el ID en el hashCode
        if (this.id != null) {
            return Objects.hash(id);
        }
        // Para objetos transientes, usamos el email
        return Objects.hash(email);
    }

    // ========================
    // toString()
    // ========================

    @Override
    public String toString() {
        return "Usuario{" +
                "id=" + id +
                ", nombre='" + nombre + '\'' +
                ", email='" + email + '\'' +
                ", rol=" + rol +
                ", activo=" + activo +
                '}';
    }
}