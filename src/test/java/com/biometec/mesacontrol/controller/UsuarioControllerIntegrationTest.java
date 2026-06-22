package com.biometec.mesacontrol.controller;

import com.biometec.mesacontrol.entity.Rol;
import com.biometec.mesacontrol.entity.Usuario;
import com.biometec.mesacontrol.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test") // Utiliza el esquema mesacontrol_test definido en application-test.yaml
@Transactional          // Realiza un ROLLBACK al final de cada test para no ensuciar la base de datos
@DisplayName("Pruebas de Integración Reales del Controlador UsuarioController")
class UsuarioControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    private Usuario usuarioPrueba;

    @BeforeEach
    void setUp() {
        // Poblamos un usuario base en la base de datos de pruebas
        // para interactuar con él en las operaciones de edición y cambio de estado
        usuarioPrueba = new Usuario("Usuario Operativo", "operativo@biometec.mx", "pwd123", Rol.VENTAS);
        usuarioPrueba = usuarioRepository.save(usuarioPrueba);
    }

    // ==========================================
    // 1. BANDEJA Y LISTADO
    // ==========================================

    @Test
    @WithMockUser(username = "admin@biometec.mx", roles = "ADMIN")
    @DisplayName("GET /usuarios: debe renderizar la vista de listado con datos de la BD real")
    void listarUsuarios_casoExitoso_debeRetornarVistaListado() throws Exception {
        mockMvc.perform(get("/usuarios"))
                .andExpect(status().isOk())
                .andExpect(view().name("usuarios/listar-usuario"))
                .andExpect(model().attributeExists("pagina"));
    }

    // ==========================================
    // 2. CREACIÓN DE USUARIO
    // ==========================================

    @Test
    @WithMockUser(username = "admin@biometec.mx", roles = "ADMIN")
    @DisplayName("GET /usuarios/nuevo: debe renderizar formulario de creación en blanco")
    void mostrarFormularioCreacion_debeCargarVista() throws Exception {
        mockMvc.perform(get("/usuarios/nuevo"))
                .andExpect(status().isOk())
                .andExpect(view().name("usuarios/formulario-usuario"))
                .andExpect(model().attributeExists("usuarioRequest", "roles"))
                .andExpect(model().attribute("modoEdicion", false));
    }

    @Test
    @WithMockUser(username = "admin@biometec.mx", roles = "ADMIN")
    @DisplayName("POST /usuarios/nuevo: debe persistir un usuario válido y redirigir con éxito")
    void procesarCreacionUsuario_casoExitoso_debeGuardarYRedirigir() throws Exception {
        mockMvc.perform(post("/usuarios/nuevo")
                        .param("nombre", "Nuevo Vendedor")
                        .param("email", "ventas.nuevo@biometec.mx")
                        .param("password", "SecurePass123!")
                        .param("rol", "VENTAS")
                        .with(csrf())) // Obligatorio para peticiones POST en Spring Security
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/usuarios"))
                .andExpect(flash().attributeExists("mensajeExito"));
    }

    @Test
    @WithMockUser(username = "admin@biometec.mx", roles = "ADMIN")
    @DisplayName("POST /usuarios/nuevo: validación preventiva (Bean Validation) debe retornar al formulario si faltan datos")
    void procesarCreacionUsuario_camposInvalidos_debeRetornarFormulario() throws Exception {
        mockMvc.perform(post("/usuarios/nuevo")
                        .param("nombre", "   ") // Forzamos fallo de @NotBlank
                        .param("email", "correo-invalido") // Formato de email incorrecto
                        // Omitimos el password intencionalmente
                        .with(csrf()))
                .andExpect(status().isOk()) // No hay redirección, se devuelve el HTML
                .andExpect(view().name("usuarios/formulario-usuario"))
                .andExpect(model().hasErrors()); // Confirmamos que los validadores interceptaron los errores
    }

    // ==========================================
    // 3. EDICIÓN DE USUARIO
    // ==========================================

    @Test
    @WithMockUser(username = "admin@biometec.mx", roles = "ADMIN")
    @DisplayName("GET /usuarios/{id}/editar: debe precargar la información del usuario en el formulario")
    void mostrarFormularioEdicion_casoExitoso_debeCargarVista() throws Exception {
        mockMvc.perform(get("/usuarios/" + usuarioPrueba.getId() + "/editar"))
                .andExpect(status().isOk())
                .andExpect(view().name("usuarios/formulario-usuario"))
                .andExpect(model().attributeExists("usuarioRequest", "usuarioId"))
                .andExpect(model().attribute("modoEdicion", true));
    }

    @Test
    @WithMockUser(username = "admin@biometec.mx", roles = "ADMIN")
    @DisplayName("POST /usuarios/{id}/editar: debe actualizar datos omitiendo password y redirigir")
    void procesarEdicionUsuario_casoExitoso_debeActualizarYRedirigir() throws Exception {
        mockMvc.perform(post("/usuarios/" + usuarioPrueba.getId() + "/editar")
                        .param("nombre", "Nombre Modificado BD")
                        .param("email", usuarioPrueba.getEmail()) // Mismo email
                        .param("rol", "TECNICO")
                        // Omitimos el password ya que el grupo @OnUpdate lo permite
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/usuarios"))
                .andExpect(flash().attributeExists("mensajeExito"));
    }

    // ==========================================
    // 4. CAMBIO DE ESTADO
    // ==========================================

    @Test
    @WithMockUser(username = "admin@biometec.mx", roles = "ADMIN")
    @DisplayName("POST /usuarios/{id}/toggle-estado: debe alternar activo/inactivo de forma segura")
    void alternarEstado_casoExitoso_debeCambiarEstadoYRedirigir() throws Exception {
        mockMvc.perform(post("/usuarios/" + usuarioPrueba.getId() + "/toggle-estado")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/usuarios"))
                .andExpect(flash().attributeExists("mensajeExito"));
    }
}