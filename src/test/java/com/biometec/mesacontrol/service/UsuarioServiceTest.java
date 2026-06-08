package com.biometec.mesacontrol.service;

import com.biometec.mesacontrol.dto.UsuarioRequestDTO;
import com.biometec.mesacontrol.dto.UsuarioResponseDTO;
import com.biometec.mesacontrol.entity.Rol;
import com.biometec.mesacontrol.entity.Usuario;
import com.biometec.mesacontrol.exception.EmailRegistradoException;
import com.biometec.mesacontrol.mapper.UsuarioMapper;
import com.biometec.mesacontrol.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    // Usamos @Spy para que ejecute el código REAL del mapper.
    // Es crucial para probar la lógica de omisión de contraseñas vacías.
    @Spy
    private UsuarioMapper usuarioMapper;

    @InjectMocks
    private UsuarioService usuarioService;

    // ==========================================
    // EDGE CASE 1: EDICIÓN CON CONTRASEÑA VACÍA
    // ==========================================
    @Test
    @DisplayName("Al editar usuario con password vacía, NO debe alterar la password existente")
    void editarUsuario_PasswordVacia_DebeConservarPasswordAnterior() {
        // Arrange (Preparación)
        Long idUsuario = 1L;
        String hashOriginal = "$2a$10$hash_antiguo_super_seguro";
        Usuario usuarioEnBD = new Usuario("Juan", "juan@biometec.mx", hashOriginal, Rol.ADMIN);
        usuarioEnBD.setId(idUsuario);

        UsuarioRequestDTO requestEdicion = new UsuarioRequestDTO();
        requestEdicion.setNombre("Juan Editado");
        requestEdicion.setEmail("juan@biometec.mx"); // Mismo email, no debe disparar validación de duplicidad
        requestEdicion.setRol(Rol.ADMIN);
        requestEdicion.setPassword(""); // El usuario dejó el campo en blanco en el formulario

        when(usuarioRepository.findById(idUsuario)).thenReturn(Optional.of(usuarioEnBD));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

        // Act (Ejecución)
        UsuarioResponseDTO resultado = usuarioService.editarUsuario(idUsuario, requestEdicion);

        // Assert (Verificación)
        verify(passwordEncoder, never()).encode(anyString()); // Verifica que JAMÁS se llamó al encriptador
        assertEquals(hashOriginal, usuarioEnBD.getPassword()); // Verifica que el hash original sigue intacto
        assertEquals("Juan Editado", resultado.nombre()); // Verifica que el resto sí se actualizó
    }

    @Test
    @DisplayName("Al editar usuario enviando nueva password, SÍ debe encriptarla y actualizarla")
    void editarUsuario_PasswordLlena_DebeActualizarPassword() {
        // Arrange
        Long idUsuario = 1L;
        Usuario usuarioEnBD = new Usuario("Juan", "juan@biometec.mx", "hash_viejo", Rol.ADMIN);
        usuarioEnBD.setId(idUsuario);

        UsuarioRequestDTO requestEdicion = new UsuarioRequestDTO();
        requestEdicion.setNombre("Juan Editado");
        requestEdicion.setEmail("juan@biometec.mx");
        requestEdicion.setRol(Rol.ADMIN);
        requestEdicion.setPassword("NuevaClave123");

        when(usuarioRepository.findById(idUsuario)).thenReturn(Optional.of(usuarioEnBD));
        when(passwordEncoder.encode("NuevaClave123")).thenReturn("hash_nuevo");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        usuarioService.editarUsuario(idUsuario, requestEdicion);

        // Assert
        verify(passwordEncoder, times(1)).encode("NuevaClave123");
        assertEquals("hash_nuevo", usuarioEnBD.getPassword());
    }

    // ==========================================
    // EDGE CASE 2: COLISIONES DE CORREO
    // ==========================================
    @Test
    @DisplayName("Al crear usuario con email ya registrado, debe lanzar EmailRegistradoException")
    void crearUsuario_EmailDuplicado_LanzaException() {
        // Arrange
        UsuarioRequestDTO nuevoUsuario = new UsuarioRequestDTO();
        nuevoUsuario.setEmail("existente@biometec.mx");
        nuevoUsuario.setPassword("12345678");

        // Simulamos que el repositorio ya encuentra a alguien con ese correo
        when(usuarioRepository.findByEmail("existente@biometec.mx")).thenReturn(Optional.of(new Usuario()));

        // Act & Assert
        assertThrows(EmailRegistradoException.class, () -> usuarioService.crearUsuario(nuevoUsuario));
        verify(usuarioRepository, never()).save(any()); // Aseguramos que nunca intentó guardarlo
    }

    @Test
    @DisplayName("Al editar email a uno ya usado por otro, debe lanzar EmailRegistradoException")
    void editarUsuario_CambioDeEmailDuplicado_LanzaException() {
        // Arrange
        Long idUsuario = 1L;
        Usuario usuarioEnBD = new Usuario("Juan", "juan@biometec.mx", "hash", Rol.ADMIN);
        usuarioEnBD.setId(idUsuario);

        UsuarioRequestDTO requestEdicion = new UsuarioRequestDTO();
        requestEdicion.setEmail("pedro_ya_existe@biometec.mx"); // Intenta robar el correo de Pedro

        when(usuarioRepository.findById(idUsuario)).thenReturn(Optional.of(usuarioEnBD));
        // Simulamos que el nuevo correo que intenta usar ya pertenece a otro
        when(usuarioRepository.findByEmail("pedro_ya_existe@biometec.mx")).thenReturn(Optional.of(new Usuario()));

        // Act & Assert
        assertThrows(EmailRegistradoException.class, () -> usuarioService.editarUsuario(idUsuario, requestEdicion));
        verify(usuarioRepository, never()).save(any());
    }

    // ==========================================
    // EDGE CASE 3: TOGGLE DE ACTIVACIÓN
    // ==========================================
    @Test
    @DisplayName("Al hacer toggle de un usuario activo, debe pasar a inactivo")
    void toggleActivacion_UsuarioActivo_DebeDesactivar() {
        // Arrange
        Long idUsuario = 1L;
        Usuario usuarioEnBD = new Usuario("Juan", "juan@biometec.mx", "hash", Rol.ADMIN);
        usuarioEnBD.setId(idUsuario);
        usuarioEnBD.setActivo(true); // Está activo

        when(usuarioRepository.findById(idUsuario)).thenReturn(Optional.of(usuarioEnBD));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        UsuarioResponseDTO resultado = usuarioService.toggleActivacion(idUsuario);

        // Assert
        assertFalse(resultado.activo()); // El DTO de respuesta dice inactivo
        assertFalse(usuarioEnBD.getActivo()); // La entidad modificada dice inactivo
        verify(usuarioRepository, times(1)).save(usuarioEnBD); // Se mandó a la BD
    }
}
