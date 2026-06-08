package com.biometec.mesacontrol.service;

import com.biometec.mesacontrol.dto.UsuarioPageResponseDTO;
import com.biometec.mesacontrol.dto.UsuarioRequestDTO;
import com.biometec.mesacontrol.dto.UsuarioResponseDTO;
import com.biometec.mesacontrol.entity.Usuario;
import com.biometec.mesacontrol.exception.EmailRegistradoException;
import com.biometec.mesacontrol.exception.UsuarioNoEncontradoException;
import com.biometec.mesacontrol.mapper.UsuarioMapper;
import com.biometec.mesacontrol.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * Servicio de negocio para la gestión de usuarios del sistema.
 *
 * <p>Actúa como capa intermedia entre el controlador y el repositorio.
 * Toda la lógica de negocio (validaciones, cifrado, activación) reside aquí.
 * Los controladores reciben y devuelven DTOs; esta clase se encarga de la
 * conversión a entidades y viceversa a través de {@link UsuarioMapper}.</p>
 *
 * <p>Todas las operaciones de escritura son transaccionales por defecto
 * gracias a {@code @Transactional} a nivel de clase. Las operaciones de
 * solo lectura sobreescriben esa configuración con {@code readOnly = true}
 * para optimizar el rendimiento.</p>
 *
 * @author Juan Jaramillo
 * @version 1.0
 * @see UsuarioRepository
 * @see UsuarioMapper
 */
@Service
@Transactional
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioMapper usuarioMapper;

    // ========================
    // LISTADO
    // ========================

    /**
     * Obtiene una página con todos los usuarios del sistema.
     *
     * @param pageable configuración de paginación y ordenamiento
     * @return página de {@link UsuarioResponseDTO} con los usuarios encontrados
     * @throws NullPointerException si {@code pageable} es {@code null}
     */
    @Transactional(readOnly = true)
    public UsuarioPageResponseDTO listarTodos(Pageable pageable) {
        Objects.requireNonNull(pageable, "El pageable no puede ser nulo");
        Page<Usuario> pagina = usuarioRepository.findAll(pageable);
        return usuarioMapper.toPageResponseDTO(pagina);
    }

    /**
     * Obtiene una página con los usuarios cuyo estado es activo.
     *
     * <p>El filtro se delega a la base de datos mediante una query derivada,
     * evitando cargar la tabla completa en memoria.</p>
     *
     * @param pageable configuración de paginación y ordenamiento
     * @return página de {@link UsuarioResponseDTO} con los usuarios activos
     * @throws NullPointerException si {@code pageable} es {@code null}
     */
    @Transactional(readOnly = true)
    public UsuarioPageResponseDTO listarActivos(Pageable pageable) {
        Objects.requireNonNull(pageable, "El pageable no puede ser nulo");
        Page<Usuario> pagina = usuarioRepository.findByActivoTrue(pageable);
        return usuarioMapper.toPageResponseDTO(pagina);
    }

    /**
     * Obtiene una página con los usuarios cuyo estado es inactivo.
     *
     * <p>El filtro se delega a la base de datos mediante una query derivada,
     * evitando cargar la tabla completa en memoria.</p>
     *
     * @param pageable configuración de paginación y ordenamiento
     * @return página de {@link UsuarioResponseDTO} con los usuarios inactivos
     * @throws NullPointerException si {@code pageable} es {@code null}
     */
    @Transactional(readOnly = true)
    public UsuarioPageResponseDTO listarInactivos(Pageable pageable) {
        Objects.requireNonNull(pageable, "El pageable no puede ser nulo");
        Page<Usuario> pagina = usuarioRepository.findByActivoFalse(pageable);
        return usuarioMapper.toPageResponseDTO(pagina);
    }

    /**
     * Busca un usuario por su identificador único y lo retorna como DTO.
     *
     * @param id identificador del usuario; no puede ser {@code null}
     * @return {@link UsuarioResponseDTO} con los datos del usuario encontrado
     * @throws NullPointerException     si {@code id} es {@code null}
     * @throws IllegalArgumentException si no existe ningún usuario con ese ID
     */
    @Transactional(readOnly = true)
    public UsuarioResponseDTO obtenerPorId(Long id) {
        Objects.requireNonNull(id, "El ID no puede ser nulo");
        Usuario usuario = buscarEntidadPorId(id);
        return usuarioMapper.toResponseDTO(usuario);
    }

    /**
     * Busca un usuario por su dirección de email y lo retorna como DTO.
     *
     * <p>La búsqueda es insensible a mayúsculas; el email se normaliza
     * a minúsculas antes de consultar el repositorio.</p>
     *
     * @param email dirección de email del usuario; no puede ser {@code null}
     * @return {@link UsuarioResponseDTO} con los datos del usuario encontrado
     * @throws NullPointerException     si {@code email} es {@code null}
     * @throws UsuarioNoEncontradoException si no existe ningún usuario con ese email
     */
    @Transactional(readOnly = true)
    public UsuarioResponseDTO obtenerPorEmail(String email) {
        Objects.requireNonNull(email, "El email no puede ser nulo");
        Usuario usuario = usuarioRepository.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new UsuarioNoEncontradoException(
                        "Usuario con email " + email + " no encontrado"));
        return usuarioMapper.toResponseDTO(usuario);
    }

    // ========================
    // ALTA (CREATE)
    // ========================

    /**
     * Crea un nuevo usuario en el sistema a partir de los datos del DTO.
     *
     * <p>El flujo es el siguiente:</p>
     * <ol>
     *   <li>El mapper convierte el {@link UsuarioRequestDTO} en una entidad.</li>
     *   <li>Se verifica que el email no esté ya registrado.</li>
     *   <li>La contraseña se cifra antes de persistir la entidad.</li>
     *   <li>Se retorna la entidad guardada como {@link UsuarioResponseDTO}.</li>
     * </ol>
     *
     * @param dto datos del nuevo usuario validados por Bean Validation
     * @return {@link UsuarioResponseDTO} con los datos del usuario creado,
     *         incluyendo el ID generado por la base de datos
     * @throws NullPointerException     si {@code dto} es {@code null}
     * @throws IllegalArgumentException si la contraseña está vacía
     * @throws EmailRegistradoException si el email ya está registrado
     */
    public UsuarioResponseDTO crearUsuario(UsuarioRequestDTO dto) {
        Objects.requireNonNull(dto, "El DTO no puede ser nulo");
        validarPasswordObligatoria(dto.getPassword());
        validarEmailUnico(dto.getEmail());

        Usuario usuario = usuarioMapper.toEntity(dto);
        usuario.setPassword(passwordEncoder.encode(dto.getPassword()));

        return usuarioMapper.toResponseDTO(usuarioRepository.save(usuario));
    }

    // ========================
    // EDICIÓN (UPDATE)
    // ========================

    /**
     * Actualiza los datos de un usuario existente.
     *
     * <p>Reglas de actualización:</p>
     * <ul>
     *   <li><b>Email:</b> si cambia, se verifica que no esté en uso por otro usuario.</li>
     *   <li><b>Contraseña:</b> solo se actualiza si se proporciona un valor no vacío;
     *       en ese caso se cifra antes de persistir.</li>
     *   <li><b>Nombre y rol:</b> se actualizan siempre que no sean nulos ni vacíos.</li>
     * </ul>
     *
     * @param id  identificador del usuario a editar; no puede ser {@code null}
     * @param dto datos actualizados del usuario validados por Bean Validation
     * @return {@link UsuarioResponseDTO} con los datos del usuario actualizado
     * @throws NullPointerException     si {@code id} o {@code dto} son {@code null}
     * @throws UsuarioNoEncontradoException si el usuario no existe
     * @throws EmailRegistradoException si el nuevo email ya está registrado por otro usuario
     */
    public UsuarioResponseDTO editarUsuario(Long id, UsuarioRequestDTO dto) {
        Objects.requireNonNull(id, "El ID no puede ser nulo");
        Objects.requireNonNull(dto, "El DTO no puede ser nulo");

        Usuario usuarioExistente = buscarEntidadPorId(id);

        // Validar email solo si realmente cambió
        if (!usuarioExistente.getEmail().equals(dto.getEmail())) {
            validarEmailUnico(dto.getEmail());
        }

        // El mapper aplica los campos del DTO sobre la entidad existente,
        // incluyendo la contraseña cruda si viene informada
        usuarioMapper.updateEntityFromDTO(dto, usuarioExistente);

        // Cifrar la contraseña si el mapper la actualizó
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            usuarioExistente.setPassword(passwordEncoder.encode(dto.getPassword()));
        }

        return usuarioMapper.toResponseDTO(usuarioRepository.save(usuarioExistente));
    }

    // ========================
    // ACTIVACIÓN / DESACTIVACIÓN
    // ========================

    /**
     * Activa un usuario que se encuentra en estado inactivo.
     *
     * @param id identificador del usuario a activar; no puede ser {@code null}
     * @return {@link UsuarioResponseDTO} con el usuario ya activado
     * @throws NullPointerException     si {@code id} es {@code null}
     * @throws UsuarioNoEncontradoException si no existe ningún usuario con ese ID
     */
    public UsuarioResponseDTO activarUsuario(Long id) {
        Usuario usuario = buscarEntidadPorId(id);
        usuario.setActivo(true);
        return usuarioMapper.toResponseDTO(usuarioRepository.save(usuario));
    }

    /**
     * Desactiva un usuario que se encuentra en estado activo.
     *
     * @param id identificador del usuario a desactivar; no puede ser {@code null}
     * @return {@link UsuarioResponseDTO} con el usuario ya desactivado
     * @throws NullPointerException     si {@code id} es {@code null}
     * @throws UsuarioNoEncontradoException si no existe ningún usuario con ese ID
     */
    public UsuarioResponseDTO desactivarUsuario(Long id) {
        Usuario usuario = buscarEntidadPorId(id);
        usuario.setActivo(false);
        return usuarioMapper.toResponseDTO(usuarioRepository.save(usuario));
    }

    /**
     * Alterna el estado de activación de un usuario.
     *
     * <p>Si el usuario estaba activo pasa a inactivo, y viceversa.</p>
     *
     * @param id identificador del usuario; no puede ser {@code null}
     * @return {@link UsuarioResponseDTO} con el estado de activación invertido
     * @throws NullPointerException     si {@code id} es {@code null}
     * @throws UsuarioNoEncontradoException si no existe ningún usuario con ese ID
     */
    public UsuarioResponseDTO toggleActivacion(Long id) {
        Usuario usuario = buscarEntidadPorId(id);
        usuario.setActivo(!usuario.getActivo());
        return usuarioMapper.toResponseDTO(usuarioRepository.save(usuario));
    }

    /**
     * Verifica si un usuario está en estado activo.
     *
     * @param id identificador del usuario; no puede ser {@code null}
     * @return {@code true} si el usuario está activo, {@code false} en caso contrario
     * @throws NullPointerException     si {@code id} es {@code null}
     * @throws UsuarioNoEncontradoException si no existe ningún usuario con ese ID
     */
    @Transactional(readOnly = true)
    public boolean estaActivo(Long id) {
        return buscarEntidadPorId(id).getActivo();
    }

    // ========================
    // ELIMINACIÓN
    // ========================

    /**
     * Elimina físicamente un usuario de la base de datos.
     *
     * <p><b>Advertencia:</b> esta operación es irreversible. Se recomienda
     * usar {@link #desactivarUsuario(Long)} como alternativa no destructiva.</p>
     *
     * @param id identificador del usuario a eliminar; no puede ser {@code null}
     * @throws NullPointerException     si {@code id} es {@code null}
     * @throws UsuarioNoEncontradoException si no existe ningún usuario con ese ID
     */
    public void eliminarUsuario(Long id) {
        Usuario usuario = buscarEntidadPorId(id);
        usuarioRepository.delete(usuario);
    }

    // ========================
    // MÉTODOS PRIVADOS
    // ========================

    /**
     * Recupera la entidad {@link Usuario} por ID o lanza excepción si no existe.
     *
     * <p>Método interno reutilizado por todas las operaciones que necesitan
     * trabajar con la entidad antes de mapearla a DTO.</p>
     *
     * @param id identificador del usuario
     * @return la entidad {@link Usuario} encontrada
     * @throws UsuarioNoEncontradoException si no existe ningún usuario con ese ID
     */
    private Usuario buscarEntidadPorId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new UsuarioNoEncontradoException(
                        "Usuario con ID " + id + " no encontrado"));
    }

    /**
     * Valida que el email no esté registrado por ningún otro usuario.
     *
     * @param email dirección de email a verificar
     * @throws EmailRegistradoException si el email ya está en uso
     */
    private void validarEmailUnico(String email) {
        if (usuarioRepository.findByEmail(email.toLowerCase()).isPresent()) {
            throw new EmailRegistradoException(
                    "El email " + email + " ya está registrado");
        }
    }

    /**
     * Valida que la contraseña esté presente en operaciones de creación.
     *
     * <p>En edición la contraseña es opcional, por eso esta validación
     * se invoca explícitamente solo desde {@link #crearUsuario(UsuarioRequestDTO)}.</p>
     *
     * @param password contraseña a validar
     * @throws IllegalArgumentException si la contraseña es {@code null} o vacía
     */
    private void validarPasswordObligatoria(String password) {
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException(
                    "La contraseña es obligatoria al crear un usuario");
        }
    }
}