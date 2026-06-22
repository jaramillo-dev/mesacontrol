package com.biometec.mesacontrol.dto;

import com.biometec.mesacontrol.entity.Rol;
import jakarta.validation.constraints.*;

/**
 * DTO utilizado para la captura y actualización de usuarios.
 *
 * <p>
 * Centraliza las reglas de validación asociadas al formulario de usuarios
 * y desacopla la capa web de las entidades persistentes.
 * </p>
 *
 * <p>
 * Utiliza grupos de validación para diferenciar los requisitos de alta
 * y modificación, especialmente para el manejo de contraseñas.
 * </p>
 *
 * @author Juan Jaramillo
 * @version 1.0
 */
public class UsuarioRequestDTO {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 2, max = 255, message = "El nombre debe tener entre 2 y 255 caracteres")
    private String nombre;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email no tiene un formato válido")
    @Size(max = 255, message = "El email no puede superar los 255 caracteres")
    private String email;

//    @Size(min = 8, max = 255, message = "La contraseña debe tener al menos 8 caracteres")
//    private String password;

    // La contraseña es obligatoria y debe medir min 8 SOLO al crear
    @NotBlank(message = "La contraseña es obligatoria", groups = OnCreate.class)
    @Size(min = 8, max = 255, message = "La contraseña debe tener al menos 8 caracteres", groups = OnCreate.class)
    // Al editar, si el usuario escribe algo, validamos que mida min 8 (Pattern evita que valide si viene vacío)
    @Pattern(regexp = "^$|.{8,255}", message = "La contraseña debe tener al menos 8 caracteres", groups = OnUpdate.class)
    private String password;

    @NotNull(message = "El rol es obligatorio")
    private Rol rol;

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public Rol getRol() { return rol; }
    public void setRol(Rol rol) { this.rol = rol; }
}
