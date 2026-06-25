package com.biometec.mesacontrol.service;

import com.biometec.mesacontrol.dto.ComentarioDTO;
import com.biometec.mesacontrol.dto.DashboardStatsDTO;
import com.biometec.mesacontrol.dto.TicketRequestDTO;
import com.biometec.mesacontrol.dto.TicketResponseDTO;
import com.biometec.mesacontrol.entity.Cliente;
import com.biometec.mesacontrol.entity.Comentario;
import com.biometec.mesacontrol.entity.Equipo;
import com.biometec.mesacontrol.entity.EstadoTicket;
import com.biometec.mesacontrol.entity.PrioridadTicket;
import com.biometec.mesacontrol.entity.Rol;
import com.biometec.mesacontrol.entity.Ticket;
import com.biometec.mesacontrol.entity.TipoTicket;
import com.biometec.mesacontrol.entity.Usuario;
import com.biometec.mesacontrol.exception.UsuarioNoEncontradoException;
import com.biometec.mesacontrol.mapper.TicketMapper;
import com.biometec.mesacontrol.repository.ClienteRepository;
import com.biometec.mesacontrol.repository.ComentarioRepository;
import com.biometec.mesacontrol.repository.EquipoRepository;
import com.biometec.mesacontrol.repository.TicketRepository;
import com.biometec.mesacontrol.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private EquipoRepository equipoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private ComentarioRepository comentarioRepository;

    @Mock
    private TicketMapper ticketMapper;

    @InjectMocks
    private TicketService ticketService;

    @Test
    @DisplayName("crearTicket debe guardar un ticket válido y devolver el DTO mapeado")
    void crearTicket_casoExitoso_debePersistirYRetornarDto() {
        // Arrange
        Cliente cliente = construirCliente(1L);
        Equipo equipo = construirEquipo(2L, cliente);
        Usuario responsable = construirUsuario(3L, "Responsable", "responsable@biometec.mx", Rol.TECNICO);
        TicketRequestDTO request = construirSolicitudTicket();
        Ticket ticketGuardado = construirTicketGuardado();
        TicketResponseDTO respuestaEsperada = construirRespuestaTicket(ticketGuardado);

        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(equipoRepository.findById(2L)).thenReturn(Optional.of(equipo));
        when(usuarioRepository.findById(3L)).thenReturn(Optional.of(responsable));
        when(ticketRepository.save(any(Ticket.class))).thenReturn(ticketGuardado);
        when(ticketMapper.toResponseDTO(ticketGuardado)).thenReturn(respuestaEsperada);

        // Act
        TicketResponseDTO resultado = ticketService.crearTicket(request);

        // Assert
        assertThat(resultado).isEqualTo(respuestaEsperada);
        verify(ticketRepository, times(1)).save(any(Ticket.class));
        verify(ticketMapper, times(1)).toResponseDTO(ticketGuardado);
    }

    @Test
    @DisplayName("crearTicket debe lanzar excepción cuando falta el cliente")
    void crearTicket_clienteInexistente_debeLanzarExcepcion() {
        // Arrange
        TicketRequestDTO request = construirSolicitudTicket();
        when(clienteRepository.findById(1L)).thenReturn(Optional.empty());

        // Act y Assert
        assertThatThrownBy(() -> ticketService.crearTicket(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("El cliente con ID 1 no existe");

        verify(equipoRepository, never()).findById(any());
        verify(usuarioRepository, never()).findById(any());
        verify(ticketRepository, never()).save(any());
    }

    @Test
    @DisplayName("crearTicket debe lanzar excepción cuando el responsable no existe")
    void crearTicket_responsableInexistente_debeLanzarExcepcion() {
        // Arrange
        Cliente cliente = construirCliente(1L);
        Equipo equipo = construirEquipo(2L, cliente);
        TicketRequestDTO request = construirSolicitudTicket();

        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(equipoRepository.findById(2L)).thenReturn(Optional.of(equipo));
        when(usuarioRepository.findById(3L)).thenReturn(Optional.empty());

        // Act y Assert
        assertThatThrownBy(() -> ticketService.crearTicket(request))
                .isInstanceOf(UsuarioNoEncontradoException.class)
                .hasMessageContaining("Usuario responsable no encontrado con ID: 3");

        verify(ticketRepository, never()).save(any());
        verify(ticketMapper, never()).toResponseDTO(any());
    }

    @Test
    @DisplayName("listarTicketsPorRol debe devolver los tickets globales para ADMIN")
    void listarTicketsPorRol_admin_debeRetornarTodosLosTickets() {
        // Arrange
        Usuario usuario = construirUsuario(10L, "Admin", "admin@biometec.mx", Rol.ADMIN);
        Pageable pageable = PageRequest.of(0, 10);
        Ticket ticket = construirTicketGuardado();
        TicketResponseDTO dto = construirRespuestaTicket(ticket);
        Page<Ticket> pagina = new PageImpl<>(List.of(ticket), pageable, 1);

        when(ticketRepository.findAll(pageable)).thenReturn(pagina);
        when(ticketMapper.toResponseDTO(ticket)).thenReturn(dto);

        // Act
        Page<TicketResponseDTO> resultado = ticketService.listarTicketsPorRol(usuario, pageable);

        // Assert
        assertThat(resultado).hasSize(1);
        assertThat(resultado.getContent()).containsExactly(dto);
        verify(ticketRepository, times(1)).findAll(pageable);
    }

    @Test
    @DisplayName("listarTicketsPorRol debe lanzar excepción con usuario nulo")
    void listarTicketsPorRol_usuarioNulo_debeLanzarExcepcion() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);

        // Act y Assert
        assertThatThrownBy(() -> ticketService.listarTicketsPorRol(null, pageable))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("El contexto de usuario autenticado es requerido");

        verify(ticketRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    @DisplayName("listarTicketsPorRol debe consultar por tipo y responsable cuando el usuario es VENTAS")
    void listarTicketsPorRol_ventas_debeFiltrarPorTipoYResponsable() {
        // Arrange
        Usuario usuario = construirUsuario(11L, "Ventas", "ventas@biometec.mx", Rol.VENTAS);
        Pageable pageable = PageRequest.of(0, 10);
        Ticket ticket = construirTicketGuardado();
        TicketResponseDTO dto = construirRespuestaTicket(ticket);
        Page<Ticket> pagina = new PageImpl<>(List.of(ticket), pageable, 1);

        when(ticketRepository.findByTipoAndResponsableId(TipoTicket.VENTA, 11L, pageable)).thenReturn(pagina);
        when(ticketMapper.toResponseDTO(ticket)).thenReturn(dto);

        // Act
        Page<TicketResponseDTO> resultado = ticketService.listarTicketsPorRol(usuario, pageable);

        // Assert
        assertThat(resultado.getContent()).containsExactly(dto);
        verify(ticketRepository, times(1)).findByTipoAndResponsableId(TipoTicket.VENTA, 11L, pageable);
    }

    @Test
    @DisplayName("listarTicketsFiltradosPorRol debe devolver tickets vencidos para ADMIN")
    void listarTicketsFiltradosPorRol_admin_vencidos_debeUsarConsultaDeVencidos() {
        // Arrange
        Usuario usuario = construirUsuario(12L, "Admin", "admin2@biometec.mx", Rol.ADMIN);
        Pageable pageable = PageRequest.of(0, 10);
        Ticket ticket = construirTicketGuardado();
        TicketResponseDTO dto = construirRespuestaTicket(ticket);
        Page<Ticket> pagina = new PageImpl<>(List.of(ticket), pageable, 1);

        when(ticketRepository.findVencidosPorFiltro(null, null, pageable)).thenReturn(pagina);
        when(ticketMapper.toResponseDTO(ticket)).thenReturn(dto);

        // Act
        Page<TicketResponseDTO> resultado = ticketService.listarTicketsFiltradosPorRol("VENCIDO", usuario, pageable);

        // Assert
        assertThat(resultado.getContent()).containsExactly(dto);
        verify(ticketRepository, times(1)).findVencidosPorFiltro(null, null, pageable);
    }

    @Test
    @DisplayName("listarTicketsFiltradosPorRol debe lanzar excepción cuando el filtro es inválido")
    void listarTicketsFiltradosPorRol_filtroInvalido_debeLanzarExcepcion() {
        // Arrange
        Usuario usuario = construirUsuario(12L, "Admin", "admin2@biometec.mx", Rol.ADMIN);
        Pageable pageable = PageRequest.of(0, 10);

        // Act y Assert
        assertThatThrownBy(() -> ticketService.listarTicketsFiltradosPorRol("NO_EXISTE", usuario, pageable))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No enum constant");
    }

    @Test
    @DisplayName("listarTicketsFiltradosPorRol debe consultar por estado para TECNICO")
    void listarTicketsFiltradosPorRol_tecnico_debeFiltrarPorEstado() {
        // Arrange
        Usuario usuario = construirUsuario(13L, "Técnico", "tecnico@biometec.mx", Rol.TECNICO);
        Pageable pageable = PageRequest.of(0, 10);
        Ticket ticket = construirTicketGuardado();
        TicketResponseDTO dto = construirRespuestaTicket(ticket);
        Page<Ticket> pagina = new PageImpl<>(List.of(ticket), pageable, 1);

        when(ticketRepository.findPorEstadoRolYResponsable(EstadoTicket.ABIERTO, 13L, TipoTicket.SERVICIO, pageable)).thenReturn(pagina);
        when(ticketMapper.toResponseDTO(ticket)).thenReturn(dto);

        // Act
        Page<TicketResponseDTO> resultado = ticketService.listarTicketsFiltradosPorRol("ABIERTO", usuario, pageable);

        // Assert
        assertThat(resultado.getContent()).containsExactly(dto);
        verify(ticketRepository, times(1)).findPorEstadoRolYResponsable(EstadoTicket.ABIERTO, 13L, TipoTicket.SERVICIO, pageable);
    }

    @Test
    @DisplayName("obtenerPorFolio debe devolver el ticket cuando existe")
    void obtenerPorFolio_casoExitoso_debeRetornarTicket() {
        // Arrange
        Ticket ticket = construirTicketGuardado();
        TicketResponseDTO dto = construirRespuestaTicket(ticket);
        when(ticketRepository.findByFolio("TK-123")).thenReturn(Optional.of(ticket));
        when(ticketMapper.toResponseDTO(ticket)).thenReturn(dto);

        // Act
        TicketResponseDTO resultado = ticketService.obtenerPorFolio("TK-123");

        // Assert
        assertThat(resultado).isEqualTo(dto);
        verify(ticketRepository, times(1)).findByFolio("TK-123");
    }

    @Test
    @DisplayName("obtenerPorFolio debe lanzar excepción cuando el folio no existe")
    void obtenerPorFolio_folioInexistente_debeLanzarExcepcion() {
        // Arrange
        when(ticketRepository.findByFolio("TK-999")).thenReturn(Optional.empty());

        // Act y Assert
        assertThatThrownBy(() -> ticketService.obtenerPorFolio("TK-999"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No se encontró ningún ticket asociado al folio: TK-999");
    }

    @Test
    @DisplayName("buscarTicketsPorFolioYRol debe consultar por folio parcial para ADMIN")
    void buscarTicketsPorFolioYRol_admin_debeBuscarGlobalmente() {
        // Arrange
        Usuario usuario = construirUsuario(14L, "Admin", "admin3@biometec.mx", Rol.ADMIN);
        Pageable pageable = PageRequest.of(0, 10);
        Ticket ticket = construirTicketGuardado();
        TicketResponseDTO dto = construirRespuestaTicket(ticket);
        Page<Ticket> pagina = new PageImpl<>(List.of(ticket), pageable, 1);

        when(ticketRepository.findByFolioContainingIgnoreCase("%TK-12%", pageable)).thenReturn(pagina);
        when(ticketMapper.toResponseDTO(ticket)).thenReturn(dto);

        // Act
        Page<TicketResponseDTO> resultado = ticketService.buscarTicketsPorFolioYRol("tk-12", usuario, pageable);

        // Assert
        assertThat(resultado.getContent()).containsExactly(dto);
        verify(ticketRepository, times(1)).findByFolioContainingIgnoreCase("%TK-12%", pageable);
    }

    @Test
    @DisplayName("buscarTicketsPorFolioYRol debe consultar por tipo y responsable para VENTAS")
    void buscarTicketsPorFolioYRol_ventas_debeUsarFiltroEspecifico() {
        // Arrange
        Usuario usuario = construirUsuario(15L, "Ventas", "ventas2@biometec.mx", Rol.VENTAS);
        Pageable pageable = PageRequest.of(0, 10);
        Ticket ticket = construirTicketGuardado();
        TicketResponseDTO dto = construirRespuestaTicket(ticket);
        Page<Ticket> pagina = new PageImpl<>(List.of(ticket), pageable, 1);

        when(ticketRepository.findByFolioAndTipoAndResponsable("%ABC%", TipoTicket.VENTA, 15L, pageable)).thenReturn(pagina);
        when(ticketMapper.toResponseDTO(ticket)).thenReturn(dto);

        // Act
        Page<TicketResponseDTO> resultado = ticketService.buscarTicketsPorFolioYRol("abc", usuario, pageable);

        // Assert
        assertThat(resultado.getContent()).containsExactly(dto);
        verify(ticketRepository, times(1)).findByFolioAndTipoAndResponsable("%ABC%", TipoTicket.VENTA, 15L, pageable);
    }

    @Test
    @DisplayName("buscarTicketsPorFolioYRol debe ignorar espacios y normalizar el folio")
    void buscarTicketsPorFolioYRol_debeNormalizarFolio() {
        // Arrange
        Usuario usuario = construirUsuario(16L, "Técnico", "tecnico2@biometec.mx", Rol.TECNICO);
        Pageable pageable = PageRequest.of(0, 10);
        Ticket ticket = construirTicketGuardado();
        TicketResponseDTO dto = construirRespuestaTicket(ticket);
        Page<Ticket> pagina = new PageImpl<>(List.of(ticket), pageable, 1);

        when(ticketRepository.findByFolioAndTipoAndResponsable("%FOLIO%", TipoTicket.SERVICIO, 16L, pageable)).thenReturn(pagina);
        when(ticketMapper.toResponseDTO(ticket)).thenReturn(dto);

        // Act
        Page<TicketResponseDTO> resultado = ticketService.buscarTicketsPorFolioYRol("  folio  ", usuario, pageable);

        // Assert
        assertThat(resultado.getContent()).containsExactly(dto);
        verify(ticketRepository, times(1)).findByFolioAndTipoAndResponsable("%FOLIO%", TipoTicket.SERVICIO, 16L, pageable);
    }

    @Test
    @DisplayName("obtenerPorId debe devolver el ticket cuando existe")
    void obtenerPorId_casoExitoso_debeRetornarTicket() {
        // Arrange
        Ticket ticket = construirTicketGuardado();
        TicketResponseDTO dto = construirRespuestaTicket(ticket);
        when(ticketRepository.findById(100L)).thenReturn(Optional.of(ticket));
        when(ticketMapper.toResponseDTO(ticket)).thenReturn(dto);

        // Act
        TicketResponseDTO resultado = ticketService.obtenerPorId(100L);

        // Assert
        assertThat(resultado).isEqualTo(dto);
    }

    @Test
    @DisplayName("obtenerPorId debe lanzar excepción cuando el ticket no existe")
    void obtenerPorId_inexistente_debeLanzarExcepcion() {
        // Arrange
        when(ticketRepository.findById(100L)).thenReturn(Optional.empty());

        // Act y Assert
        assertThatThrownBy(() -> ticketService.obtenerPorId(100L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No se encontró ningún ticket con el ID: 100");
    }

    @Test
    @DisplayName("listarTicketsCombinados debe combinar filtros para ADMIN")
    void listarTicketsCombinados_admin_debeEnviarFiltrosCompletos() {
        // Arrange
        Usuario usuario = construirUsuario(17L, "Admin", "admin4@biometec.mx", Rol.ADMIN);
        Pageable pageable = PageRequest.of(0, 10);
        Ticket ticket = construirTicketGuardado();
        TicketResponseDTO dto = construirRespuestaTicket(ticket);
        Page<Ticket> pagina = new PageImpl<>(List.of(ticket), pageable, 1);

        // Mantenemos false en el repositorio ya que no se solicita buscar vencidos
        when(ticketRepository.findTicketsCombinados("%TK%", EstadoTicket.ABIERTO, null, null, false, pageable)).thenReturn(pagina);
        when(ticketMapper.toResponseDTO(ticket)).thenReturn(dto);

        // Act -> CORRECCIÓN: Se añade el argumento 'false' antes del usuario logueado
        Page<TicketResponseDTO> resultado = ticketService.listarTicketsCombinados("tk", "ABIERTO", false, usuario, pageable);

        // Assert
        assertThat(resultado.getContent()).containsExactly(dto);
        verify(ticketRepository, times(1)).findTicketsCombinados("%TK%", EstadoTicket.ABIERTO, null, null, false, pageable);
    }

    @Test
    @DisplayName("listarTicketsCombinados debe tratar el filtro vencido como bandera especial")
    void listarTicketsCombinados_vencido_debeActivarBanderaEspecial() {
        // Arrange
        Usuario usuario = construirUsuario(18L, "Ventas", "ventas3@biometec.mx", Rol.VENTAS);
        Pageable pageable = PageRequest.of(0, 10);
        Ticket ticket = construirTicketGuardado();
        TicketResponseDTO dto = construirRespuestaTicket(ticket);
        Page<Ticket> pagina = new PageImpl<>(List.of(ticket), pageable, 1);

        // El repositorio espera true en el flag de vencimiento
        when(ticketRepository.findTicketsCombinados(null, null, TipoTicket.VENTA, 18L, true, pageable)).thenReturn(pagina);
        when(ticketMapper.toResponseDTO(ticket)).thenReturn(dto);

        // Act -> CORRECCIÓN: El segundo parámetro pasa a ser 'null' (o cadena vacía) y activamos el flag con 'true'
        Page<TicketResponseDTO> resultado = ticketService.listarTicketsCombinados(null, null, true, usuario, pageable);

        // Assert
        assertThat(resultado.getContent()).containsExactly(dto);
        verify(ticketRepository, times(1)).findTicketsCombinados(null, null, TipoTicket.VENTA, 18L, true, pageable);
    }

    @Test
    @DisplayName("iniciarAtencion debe cambiar el estado a EN_PROCESO")
    void iniciarAtencion_casoExitoso_debeActualizarEstado() {
        // Arrange
        Ticket ticket = construirTicketGuardado();
        when(ticketRepository.findById(200L)).thenReturn(Optional.of(ticket));

        // Act
        ticketService.iniciarAtencion(200L);

        // Assert
        assertThat(ticket.getEstado()).isEqualTo(EstadoTicket.EN_PROCESO);
    }

    @Test
    @DisplayName("iniciarAtencion debe lanzar excepción cuando el ticket no existe")
    void iniciarAtencion_inexistente_debeLanzarExcepcion() {
        // Arrange
        when(ticketRepository.findById(200L)).thenReturn(Optional.empty());

        // Act y Assert
        assertThatThrownBy(() -> ticketService.iniciarAtencion(200L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No se encontró el ticket con ID: 200");
    }

    @Test
    @DisplayName("iniciarAtencion debe respetar la validación de transición de la entidad")
    void iniciarAtencion_ticketCerrado_debePropagarErrorDeTransicion() {
        // Arrange
        Ticket ticket = construirTicketGuardado();
        ticket.setEstado(EstadoTicket.EN_PROCESO);
        ticket.setEstado(EstadoTicket.CERRADO);
        when(ticketRepository.findById(200L)).thenReturn(Optional.of(ticket));

        // Act y Assert
        assertThatThrownBy(() -> ticketService.iniciarAtencion(200L))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("cerrarTicket debe cerrar un ticket de venta y guardar folio de cotización")
    void cerrarTicket_ticketVenta_debeGuardarFolioCotizacion() {
        // Arrange
        Ticket ticket = construirTicketGuardado();
        when(ticketRepository.findById(300L)).thenReturn(Optional.of(ticket));

        // Act
        ticketService.cerrarTicket(300L, " COT-100 ");

        // Assert
        assertThat(ticket.getEstado()).isEqualTo(EstadoTicket.CERRADO);
        assertThat(ticket.getFolioCotizacion()).isEqualTo("COT-100");
        assertThat(ticket.getFolioOrdenServicio()).isNull();
    }

    @Test
    @DisplayName("cerrarTicket debe cerrar un ticket de servicio y guardar folio de orden")
    void cerrarTicket_ticketServicio_debeGuardarFolioOrdenServicio() {
        // Arrange
        Ticket ticket = construirTicketGuardado();
        ticket.setTipo(TipoTicket.SERVICIO);
        when(ticketRepository.findById(301L)).thenReturn(Optional.of(ticket));

        // Act
        ticketService.cerrarTicket(301L, " OS-200 ");

        // Assert
        assertThat(ticket.getEstado()).isEqualTo(EstadoTicket.CERRADO);
        assertThat(ticket.getFolioOrdenServicio()).isEqualTo("OS-200");
        assertThat(ticket.getFolioCotizacion()).isNull();
    }

    @Test
    @DisplayName("cerrarTicket debe lanzar excepción cuando el folio del documento está vacío")
    void cerrarTicket_folioVacio_debeLanzarExcepcion() {
        // Arrange
        // Sin preparación adicional

        // Act y Assert
        assertThatThrownBy(() -> ticketService.cerrarTicket(300L, "   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("El folio del documento es obligatorio para cerrar el ticket");
    }

    @Test
    @DisplayName("agregarComentario debe guardar el comentario cuando los datos son válidos")
    void agregarComentario_casoExitoso_debeGuardarComentario() {
        // Arrange
        Ticket ticket = construirTicketGuardado();
        Usuario autor = construirUsuario(400L, "Autor", "autor@biometec.mx", Rol.TECNICO);
        ArgumentCaptor<Comentario> captor = ArgumentCaptor.forClass(Comentario.class);

        when(ticketRepository.findById(400L)).thenReturn(Optional.of(ticket));
        when(usuarioRepository.findByEmail("autor@biometec.mx")).thenReturn(Optional.of(autor));
        when(comentarioRepository.save(any(Comentario.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ticketService.agregarComentario(400L, "autor@biometec.mx", " Comentario de prueba ");

        // Assert
        verify(comentarioRepository, times(1)).save(captor.capture());
        assertThat(captor.getValue().getTexto()).isEqualTo("Comentario de prueba");
        assertThat(captor.getValue().getTicket()).isSameAs(ticket);
        assertThat(captor.getValue().getAutor()).isSameAs(autor);
    }

    @Test
    @DisplayName("agregarComentario debe lanzar excepción cuando el texto está vacío")
    void agregarComentario_textoVacio_debeLanzarExcepcion() {
        // Arrange
        // Sin preparación adicional

        // Act y Assert
        assertThatThrownBy(() -> ticketService.agregarComentario(400L, "autor@biometec.mx", "   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("El comentario no puede estar vacío");
    }

    @Test
    @DisplayName("agregarComentario debe lanzar excepción cuando el autor no existe")
    void agregarComentario_autorInexistente_debeLanzarExcepcion() {
        // Arrange
        Ticket ticket = construirTicketGuardado();
        when(ticketRepository.findById(400L)).thenReturn(Optional.of(ticket));
        when(usuarioRepository.findByEmail("autor@biometec.mx")).thenReturn(Optional.empty());

        // Act y Assert
        assertThatThrownBy(() -> ticketService.agregarComentario(400L, "autor@biometec.mx", "Comentario válido"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Autor no encontrado en el sistema");
    }

    @Test
    @DisplayName("obtenerComentariosPorTicket debe mapear el historial en orden ascendente")
    void obtenerComentariosPorTicket_casoExitoso_debeMapearComentarios() {
        // Arrange
        Ticket ticket = construirTicketGuardado();
        Usuario autor = construirUsuario(500L, "Autor", "autor2@biometec.mx", Rol.ADMIN);
        Comentario comentario = new Comentario(ticket, autor, "Comentario histórico");
        comentarioRepository.save(comentario);
        List<Comentario> comentarios = List.of(comentario);

        when(comentarioRepository.findByTicketIdOrderByFechaCreacionAsc(500L)).thenReturn(comentarios);

        // Act
        List<ComentarioDTO> resultado = ticketService.obtenerComentariosPorTicket(500L);

        // Assert
        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).texto()).isEqualTo("Comentario histórico");
        assertThat(resultado.get(0).autorNombre()).isEqualTo("Autor");
    }

    @Test
    @DisplayName("obtenerComentariosPorTicket debe devolver lista vacía cuando no hay comentarios")
    void obtenerComentariosPorTicket_sinComentarios_debeRetornarListaVacia() {
        // Arrange
        when(comentarioRepository.findByTicketIdOrderByFechaCreacionAsc(500L)).thenReturn(List.of());

        // Act
        List<ComentarioDTO> resultado = ticketService.obtenerComentariosPorTicket(500L);

        // Assert
        assertThat(resultado).isEmpty();
    }

    @Test
    @DisplayName("obtenerTicketsParaDashboard debe devolver las estadísticas del usuario autenticado")
    void obtenerTicketsParaDashboard_casoExitoso_debeRetornarEstadisticas() {
        // Arrange
        Usuario usuario = construirUsuario(600L, "Usuario", "usuario@biometec.mx", Rol.VENTAS);
        DashboardStatsDTO stats = new DashboardStatsDTO(1L, 2L, 3L, 4L);

        when(usuarioRepository.findByEmail("usuario@biometec.mx")).thenReturn(Optional.of(usuario));
        when(ticketRepository.obtenerEstadisticasDashboard(600L, EstadoTicket.ABIERTO, EstadoTicket.EN_PROCESO, EstadoTicket.CERRADO))
                .thenReturn(stats);

        // Act
        DashboardStatsDTO resultado = ticketService.obtenerTicketsParaDashboard("usuario@biometec.mx");

        // Assert
        assertThat(resultado).isEqualTo(stats);
    }

    @Test
    @DisplayName("obtenerTicketsParaDashboard debe lanzar excepción cuando el nombre de usuario está vacío")
    void obtenerTicketsParaDashboard_nombreVacio_debeLanzarExcepcion() {
        // Arrange
        // Sin preparación adicional

        // Act y Assert
        assertThatThrownBy(() -> ticketService.obtenerTicketsParaDashboard("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("El nombre de usuario no puede estar vacío");
    }

    @Test
    @DisplayName("obtenerTicketsParaDashboard debe usar filtro nulo para ADMIN")
    void obtenerTicketsParaDashboard_admin_debeConsultarSinFiltroDeResponsable() {
        // Arrange
        Usuario usuario = construirUsuario(601L, "Admin", "admin5@biometec.mx", Rol.ADMIN);
        DashboardStatsDTO stats = new DashboardStatsDTO(10L, 20L, 30L, 40L);

        when(usuarioRepository.findByEmail("admin5@biometec.mx")).thenReturn(Optional.of(usuario));
        when(ticketRepository.obtenerEstadisticasDashboard(null, EstadoTicket.ABIERTO, EstadoTicket.EN_PROCESO, EstadoTicket.CERRADO))
                .thenReturn(stats);

        // Act
        DashboardStatsDTO resultado = ticketService.obtenerTicketsParaDashboard("admin5@biometec.mx");

        // Assert
        assertThat(resultado).isEqualTo(stats);
        verify(ticketRepository, times(1)).obtenerEstadisticasDashboard(null, EstadoTicket.ABIERTO, EstadoTicket.EN_PROCESO, EstadoTicket.CERRADO);
    }

    private TicketRequestDTO construirSolicitudTicket() {
        TicketRequestDTO request = new TicketRequestDTO();
        request.setTipo(TipoTicket.VENTA);
        request.setPrioridad(PrioridadTicket.ALTA);
        request.setDescripcionInicial("Descripción inicial suficientemente larga");
        request.setClienteId(1L);
        request.setEquipoId(2L);
        request.setResponsableId(3L);
        return request;
    }

    private Ticket construirTicketGuardado() {
        Cliente cliente = construirCliente(1L);
        Equipo equipo = construirEquipo(2L, cliente);
        Ticket ticket = new Ticket(
                "TK-20260620120000000",
                TipoTicket.VENTA,
                PrioridadTicket.ALTA,
                "Descripción inicial suficientemente larga",
                cliente,
                equipo,
                LocalDateTime.now().plusHours(24)
        );
        ticket.setId(100L);
        return ticket;
    }

    private TicketResponseDTO construirRespuestaTicket(Ticket ticket) {
        TicketResponseDTO dto = new TicketResponseDTO();
        dto.setId(ticket.getId());
        dto.setFolio(ticket.getFolio());
        dto.setTipo(ticket.getTipo());
        dto.setPrioridad(ticket.getPrioridad());
        dto.setEstado(ticket.getEstado());
        dto.setDescripcionInicial(ticket.getDescripcionInicial());
        dto.setFechaCreacion(ticket.getFechaCreacion());
        dto.setFechaVencimientoSla(ticket.getFechaVencimientoSla());
        dto.setFechaCierre(ticket.getFechaCierre());
        dto.setFolioCotizacion(ticket.getFolioCotizacion());
        dto.setFolioOrdenServicio(ticket.getFolioOrdenServicio());
        return dto;
    }

    private Cliente construirCliente(Long id) {
        Cliente cliente = new Cliente("Hospital Central", "Ciudad");
        cliente.setId(id);
        return cliente;
    }

    private Equipo construirEquipo(Long id, Cliente cliente) {
        Equipo equipo = new Equipo(cliente, "Resonador", "Philips", "Modelo X", "SER-001");
        equipo.setId(id);
        return equipo;
    }

    private Usuario construirUsuario(Long id, String nombre, String email, Rol rol) {
        Usuario usuario = new Usuario(nombre, email, "Password123!", rol);
        usuario.setId(id);
        return usuario;
    }
}
