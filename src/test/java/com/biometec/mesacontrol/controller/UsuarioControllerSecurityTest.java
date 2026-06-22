package com.biometec.mesacontrol.controller;

import com.biometec.mesacontrol.config.SecurityConfig;
import com.biometec.mesacontrol.dto.UsuarioPageResponseDTO;
import com.biometec.mesacontrol.dto.UsuarioResponseDTO;
import com.biometec.mesacontrol.entity.Rol;
import com.biometec.mesacontrol.mapper.UsuarioMapper;
import com.biometec.mesacontrol.security.AppUserDetailsService;
import com.biometec.mesacontrol.service.UsuarioService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;

import org.springframework.data.domain.Pageable;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Pruebas de integración ligeras centradas EXCLUSIVAMENTE en la seguridad.
 * Validamos que los roles VENTAS y TECNICO no puedan colarse a la gestión de usuarios.
 */
@WebMvcTest(UsuarioController.class)
@Import(SecurityConfig.class) // Importamos tu configuración real de seguridad
class UsuarioControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    // Mockeamos las dependencias para que el contexto de Spring cargue sin errores.
    // No necesitamos configurar su comportamiento porque la petición será rechazada
    // por Spring Security antes de llegar al controlador.
    @MockitoBean
    private UsuarioService usuarioService;

    @MockitoBean
    private UsuarioMapper usuarioMapper;

    @MockitoBean
    private AppUserDetailsService appUserDetailsService;

    // ==========================================
    // EDGE CASE 1: ACCESO DENEGADO POR ROL
    // ==========================================

    @Test
    @WithMockUser(username = "ventas@biometec.mx", roles = {"VENTAS"})
    @DisplayName("Un usuario de VENTAS no puede acceder al listado de usuarios (GET)")
    void listarTodos_RolVentas_DebeRetornarAccesoDenegado() throws Exception {
        mockMvc.perform(get("/usuarios"))
                .andExpect(status().isForbidden()) // Esperamos el 403 directo del GlobalExceptionHandler
                .andExpect(view().name("error/manejador-global"))
                .andExpect(model().attribute("codigo", 403));
    }

    // ==========================================
    // EDGE CASE 2: ACCESO SIN AUTENTICACIÓN (REFACTURADO)
    // ==========================================
    @Test
    @DisplayName("Un usuario anónimo que intenta acceder a /usuarios es redirigido al login")
    void listarTodos_UsuarioAnonimo_DebeRedirigirAlLogin() throws Exception {
        mockMvc.perform(get("/usuarios"))
                .andExpect(status().is3xxRedirection())
                // Tus logs confirman que tu SecurityConfig redirige exactamente a '/error/401'
                .andExpect(redirectedUrl("http://localhost/login"));
    }

    // ==========================================
    // EDGE CASE 3: HAPPY PATH - ADMIN (REFINADO PARA THYMELEAF)
    // ==========================================

    @Test
    @WithMockUser(username = "admin@biometec.mx", roles = {"ADMIN"})
    @DisplayName("Un usuario ADMIN SÍ puede acceder al listado de usuarios y renderizar la vista")
    void listarTodos_RolAdmin_DebePermitirAcceso() throws Exception {

        // 1. Preparamos datos ficticios para el contenido de la página
        List<UsuarioResponseDTO> usuariosFake = List.of(
                new UsuarioResponseDTO(
                        1L,
                        "Juan Admin",
                        "admin@biometec.mx",
                        Rol.ADMIN,
                        true
                )
        );

        // 2. Crear el DTO de paginación real para que Thymeleaf no reciba un null.
        UsuarioPageResponseDTO mockPagina =
                new UsuarioPageResponseDTO(
                        usuariosFake, // contenido
                        0,            // paginaActual
                        1,           // totalPaginas
                        1L,           // totalElementos
                        10           // tamanoPagina
                );

        // 3. Le indicamos al mock del servicio que devuelva esta página armada cuando el controlador la pida
        when(usuarioService.listarTodos(any(Pageable.class)))
                .thenReturn(mockPagina);

        // 4. Ejecutamos la petición
        mockMvc.perform(get("/usuarios"))
                .andExpect(status().isOk())
                .andExpect(view().name("usuarios/listar-usuario")) // Asegura que va al template correcto
                .andExpect(model().attributeExists("pagina")); // Verifica que el modelo lleva los datos de paginación
    }




}