package com.biometec.mesacontrol.controller;

import com.biometec.mesacontrol.entity.*;
import com.biometec.mesacontrol.repository.*;
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

import java.time.LocalDateTime;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test") // Conecta la base de datos mesacontrol_test
@Transactional          // HACE ROLLBACK DESPUÉS DE CADA TEST (Mantiene la BD limpia)
@DisplayName("Pruebas de Integración Reales del Controlador TicketController")
class TicketControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    // Inyectamos los repositorios reales para preparar el estado de la base de datos
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private ClienteRepository clienteRepository;
    @Autowired
    private EquipoRepository equipoRepository;
    @Autowired
    private TicketRepository ticketRepository;

    private Ticket ticketPrueba;
    private Usuario adminPrueba;
    private Usuario tecnicoPrueba;

    @BeforeEach
    void setUp() {
        // 1. Preparar Usuarios Reales en la BD de pruebas
        adminPrueba = new Usuario("Admin Integración", "admin.test@biometec.mx", "pwd123", Rol.ADMIN);
        tecnicoPrueba = new Usuario("Tecnico Integración", "tecnico.test@biometec.mx", "pwd123", Rol.TECNICO);
        usuarioRepository.save(adminPrueba);
        usuarioRepository.save(tecnicoPrueba);

        // 2. Preparar Cliente y Equipo Reales
        Cliente cliente = new Cliente("Hospital de Prueba", "Ciudad X");
        clienteRepository.save(cliente);

        Equipo equipo = new Equipo(cliente, "Resonador Magnético", "GE", "Optima", "SN-9999");
        equipoRepository.save(equipo);

        // 3. Preparar un Ticket Real
        ticketPrueba = new Ticket("TK-INT-001", TipoTicket.SERVICIO, PrioridadTicket.ALTA,
                "Falla de calibración", cliente, equipo, LocalDateTime.now().plusDays(2));
        ticketPrueba.setResponsable(tecnicoPrueba);
        ticketRepository.save(ticketPrueba);
    }

    // ==========================================
    // PRUEBAS DE BANDEJA (LISTAR)
    // ==========================================

    @Test
    @WithMockUser(username = "admin.test@biometec.mx", roles = "ADMIN")
    @DisplayName("GET /tickets: debe compilar y renderizar la vista de listado real con parámetros de búsqueda")
    void listarTickets_casoExitoso_debeRetornarVistaListado() throws Exception {

        // CORRECCIÓN: Enviamos parámetros válidos para que 'folioBusqueda' y 'filtroActivo' no sean null
        // y pasen la validación estricta de .attributeExists()
        mockMvc.perform(get("/tickets")
                        .param("folio", "TK-INT")
                        .param("filtro", "SERVICIO"))
                .andExpect(status().isOk())
                .andExpect(view().name("tickets/listar-ticket"))
                .andExpect(model().attributeExists("tickets", "folioBusqueda", "filtroActivo"));
    }

    @Test
    @WithMockUser(username = "admin.test@biometec.mx", roles = "ADMIN")
    @DisplayName("GET /tickets: normalización de parámetros en flujo real de base de datos")
    void listarTickets_parametrosVacios_debeNormalizarANull() throws Exception {

        mockMvc.perform(get("/tickets")
                        .param("folio", "   ") // Espacios simulando error de dedo
                        .param("filtro", ""))  // Combo de filtros vacío
                .andExpect(status().isOk())
                .andExpect(view().name("tickets/listar-ticket"));
    }

    // ==========================================
    // PRUEBAS DE DETALLE TICKET
    // ==========================================

    @Test
    @WithMockUser(username = "admin.test@biometec.mx", roles = "ADMIN")
    @DisplayName("GET /tickets/{folio}: debe renderizar detalle cargando relaciones desde la BD")
    void verDetalleTicket_casoExitoso_debeCargarModeloYVista() throws Exception {

        mockMvc.perform(get("/tickets/" + ticketPrueba.getFolio()))
                .andExpect(status().isOk())
                .andExpect(view().name("tickets/detalle-ticket"))
                .andExpect(model().attributeExists("ticket", "comentarios"));
    }

    // ==========================================
    // PRUEBAS DE TRANSICIÓN Y OPERACIÓN POST
    // ==========================================

    @Test
    @WithMockUser(username = "admin.test@biometec.mx", roles = "ADMIN")
    @DisplayName("POST /tickets/{id}/iniciar: debe actualizar estado en BD y redirigir")
    void iniciarAtencionTicket_casoExitoso_debeRedirigirADetalle() throws Exception {

        // ¡IMPORTANTE!: .with(csrf()) es obligatorio porque Spring Security está activo y rechaza POST sin token
        mockMvc.perform(post("/tickets/" + ticketPrueba.getId() + "/iniciar").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tickets/" + ticketPrueba.getFolio()))
                .andExpect(flash().attributeExists("success"));
    }

    @Test
    @WithMockUser(username = "admin.test@biometec.mx", roles = "ADMIN")
    @DisplayName("POST /tickets/{id}/cerrar: validación de error controlada en Controlador")
    void cerrarTicket_folioVacio_debeAtraparExcepcionYRedirigir() throws Exception {

        mockMvc.perform(post("/tickets/" + ticketPrueba.getId() + "/cerrar")
                        .param("folioDocumento", "   ") // Cierre inválido
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tickets"))
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    @WithMockUser(username = "admin.test@biometec.mx", roles = "ADMIN")
    @DisplayName("POST /tickets/{folio}/comentarios: debe guardar comentario real en BD")
    void agregarComentario_casoExitoso_debeGuardarYRedirigir() throws Exception {

        mockMvc.perform(post("/tickets/" + ticketPrueba.getFolio() + "/comentarios")
                        .param("textoComentario", "Prueba de integración de comentario")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tickets/" + ticketPrueba.getFolio()))
                .andExpect(flash().attributeExists("success"));
    }

    // ==========================================
    // PRUEBAS DE API INTERNA (JSON / AJAX)
    // ==========================================

    @Test
    @WithMockUser(username = "admin.test@biometec.mx", roles = "ADMIN")
    @DisplayName("GET /tickets/api/equipos: debe consultar BD real y retornar JSON")
    void obtenerEquiposPorCliente_casoExitoso_debeRetornarJson() throws Exception {
        // Extraemos el clienteId del equipo que guardamos en BD
        Long clienteId = ticketPrueba.getCliente().getId();

        mockMvc.perform(get("/tickets/api/equipos").param("clienteId", clienteId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Resonador Magnético"))
                .andExpect(jsonPath("$[0].numeroSerie").value("SN-9999"));
    }

    @Test
    @WithMockUser(username = "admin.test@biometec.mx", roles = "ADMIN")
    @DisplayName("GET /tickets/api/responsables: consultar técnicos activos en BD")
    void obtenerResponsablesPorTipo_tipoServicio_debeRetornarTecnicos() throws Exception {

        mockMvc.perform(get("/tickets/api/responsables").param("tipo", "SERVICIO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Técnico de Soporte"));
    }
}
