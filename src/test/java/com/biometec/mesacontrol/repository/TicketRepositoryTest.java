package com.biometec.mesacontrol.repository;

import com.biometec.mesacontrol.dto.DashboardStatsDTO;
import com.biometec.mesacontrol.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@DisplayName("Pruebas de Integración de Persistencia para TicketRepository")
class TicketRepositoryTest {

    private final TicketRepository ticketRepository;
    private final TestEntityManager entityManager;

    private Usuario tecnicoResponsable;
    private Usuario ventasResponsable;
    private Cliente clienteImss;
    private Equipo equipoMedico;
    private Pageable defaultPageable;

    @Autowired
    public TicketRepositoryTest(TicketRepository ticketRepository, TestEntityManager entityManager) {
        this.ticketRepository = ticketRepository;
        this.entityManager = entityManager;
    }

    @BeforeEach
    void setUp() {
        defaultPageable = PageRequest.of(0, 10);

        tecnicoResponsable = new Usuario("Ing. Juan Pérez", "juan.perez@biometec.mx", "Secure123!", Rol.TECNICO);
        ventasResponsable = new Usuario("Lic. Ana Gómez", "ana.gomez@biometec.mx", "Secure123!", Rol.VENTAS);
        entityManager.persist(tecnicoResponsable);
        entityManager.persist(ventasResponsable);

        clienteImss = new Cliente("HGZ 1 IMSS", "CDMX Norte");
        entityManager.persist(clienteImss);

        equipoMedico = new Equipo(clienteImss, "Ultrasonido", "GE", "Vivid E90", "SN-GE7788");
        entityManager.persist(equipoMedico);

        entityManager.flush();
    }

    @Test
    @DisplayName("findTicketsCombinados debe filtrar correctamente por folio parcial y coincidencia de tipo")
    void findTicketsCombinados_filtrarPorFolioYTipo_debeRetornarRegistrosFiltrados() {
        // Arrange
        Ticket ticketVenta = new Ticket("TK-VENTA-2026", TipoTicket.VENTA, PrioridadTicket.MEDIA, "Falla menor", clienteImss, equipoMedico, LocalDateTime.now().plusDays(2));
        ticketVenta.setResponsable(ventasResponsable);

        Ticket ticketServicio = new Ticket("TK-SERV-2026", TipoTicket.SERVICIO, PrioridadTicket.ALTA, "Mantenimiento", clienteImss, equipoMedico, LocalDateTime.now().plusDays(1));
        ticketServicio.setResponsable(tecnicoResponsable);

        entityManager.persist(ticketVenta);
        entityManager.persist(ticketServicio);
        entityManager.flush();

        // Act
        Page<Ticket> resultado = ticketRepository.findTicketsCombinados("%VENTA%", null, TipoTicket.VENTA, null, false, defaultPageable);

        // Assert
        assertThat(resultado.getContent())
                .hasSize(1)
                .extracting(Ticket::getFolio)
                .containsExactly("TK-VENTA-2026");
    }

    @Test
    @DisplayName("findTicketsCombinados debe activar la bandera 'esVencido' ignorando los tickets CERRADOS")
    void findTicketsCombinados_conFiltroVencidos_debeIgnorarTicketsCerrados() {
        // Arrange - Para simular un ticket vencido sin romper la regla (SLA < Creación),
        // creamos el ticket con un SLA en el futuro sutil (ej: +1 segundo), y esperamos un instante o alteramos
        // el registro en base de datos. Una forma limpia y rápida para JPA es usar una fecha mínima válida
        // que coincida con la creación pero que expire de inmediato, o usar un truco nativo de BD.
        // Como la validación de la entidad es (SLA < Creación), pasamos un SLA idéntico o ligeramente superior,
        // pero para que la consulta (SLA < CURRENT_TIMESTAMP) sea verdadera en la BD, hacemos que el ticket tenga un SLA sutilmente desfasado si la entidad lo permite.
        // Si tu entidad usa LocalDateTime.now() interno para la creación, pasar un SLA de LocalDateTime.now() puede fallar por milisegundos.
        // La solución ideal para probar "vencidos" en base de datos sin alterar la entidad es guardar el ticket con un SLA normal, y luego modificar la columna mediante el EntityManager nativo para forzar el estado vencido en BD.

        Ticket ticketVencidoAbierto = new Ticket("TK-VENCIDO-AB", TipoTicket.SERVICIO, PrioridadTicket.ALTA, "Urgente", clienteImss, equipoMedico, LocalDateTime.now().plusHours(1));
        ticketVencidoAbierto.setResponsable(tecnicoResponsable);

        Ticket ticketVencidoCerrado = new Ticket("TK-VENCIDO-CE", TipoTicket.SERVICIO, PrioridadTicket.ALTA, "Resuelto", clienteImss, equipoMedico, LocalDateTime.now().plusHours(1));
        ticketVencidoCerrado.setResponsable(tecnicoResponsable);

        entityManager.persist(ticketVencidoAbierto);
        entityManager.persist(ticketVencidoCerrado);
        entityManager.flush();

        // TRUCO DE PERSISTENCIA: Modificamos los valores directamente en la BD a través de una Query nativa
        // para saltarnos temporalmente las validaciones de negocio exclusivas de la JVM y poder probar la consulta SQL/JPQL.
        entityManager.getEntityManager()
                .createQuery("UPDATE Ticket t SET t.fechaVencimientoSla = :pasado WHERE t.folio IN ('TK-VENCIDO-AB', 'TK-VENCIDO-CE')")
                .setParameter("pasado", LocalDateTime.now().minusDays(1))
                .executeUpdate();

        // Transicionamos el segundo ticket a CERRADO usando la lógica permitida
        ticketVencidoCerrado.setEstado(EstadoTicket.EN_PROCESO);
        ticketVencidoCerrado.setEstado(EstadoTicket.CERRADO);
        entityManager.merge(ticketVencidoCerrado);

        entityManager.flush();
        entityManager.clear(); // Limpiamos caché de primer nivel para forzar la lectura de BD

        // Act
        Page<Ticket> resultado = ticketRepository.findTicketsCombinados(null, null, null, null, true, defaultPageable);

        // Assert
        assertThat(resultado.getContent())
                .hasSize(1)
                .extracting(Ticket::getFolio)
                .containsExactly("TK-VENCIDO-AB");
    }

    @Test
    @DisplayName("obtenerEstadisticasDashboard debe calcular correctamente las métricas globales para el rol ADMIN")
    void obtenerEstadisticasDashboard_rolAdmin_debeRetornarMetricasGlobales() {
        // Arrange
        Ticket t1 = new Ticket("TK-01", TipoTicket.VENTA, PrioridadTicket.ALTA, "Desc", clienteImss, equipoMedico, LocalDateTime.now().plusDays(1));
        t1.setResponsable(ventasResponsable);

        Ticket t2 = new Ticket("TK-02", TipoTicket.SERVICIO, PrioridadTicket.MEDIA, "Desc", clienteImss, equipoMedico, LocalDateTime.now().plusDays(2));
        t2.setEstado(EstadoTicket.EN_PROCESO); // Transición válida: ABIERTO -> EN_PROCESO
        t2.setResponsable(tecnicoResponsable);

        Ticket t3 = new Ticket("TK-03", TipoTicket.SERVICIO, PrioridadTicket.ALTA, "Desc", clienteImss, equipoMedico, LocalDateTime.now().plusDays(1));
        t3.setResponsable(tecnicoResponsable);

        entityManager.persist(t1);
        entityManager.persist(t2);
        entityManager.persist(t3);
        entityManager.flush();

        // Forzamos el vencimiento del t3 en BD de forma segura
        entityManager.getEntityManager()
                .createQuery("UPDATE Ticket t SET t.fechaVencimientoSla = :pasado WHERE t.folio = 'TK-03'")
                .setParameter("pasado", LocalDateTime.now().minusDays(1))
                .executeUpdate();

        entityManager.flush();
        entityManager.clear();

        // Act
        DashboardStatsDTO stats = ticketRepository.obtenerEstadisticasDashboard(null, EstadoTicket.ABIERTO, EstadoTicket.EN_PROCESO, EstadoTicket.CERRADO);

        // Assert
        assertThat(stats).isNotNull();
        assertThat(stats.abiertos()).isEqualTo(2L);   // t1 y t3
        assertThat(stats.enProceso()).isEqualTo(1L);  // t2
        assertThat(stats.cerrados()).isEqualTo(0L);
        assertThat(stats.vencidos()).isEqualTo(1L);   // t3
    }

    @Test
    @DisplayName("obtenerEstadisticasDashboard debe segmentar estrictamente las métricas por el ID del responsable operativo")
    void obtenerEstadisticasDashboard_filtradoPorResponsable_debeAislarCargaDeTrabajo() {
        // Arrange
        Ticket tTecnico = new Ticket("TK-TEC", TipoTicket.SERVICIO, PrioridadTicket.MEDIA, "Desc", clienteImss, equipoMedico, LocalDateTime.now().plusDays(1));
        tTecnico.setResponsable(tecnicoResponsable); // Nace ABIERTO por defecto

        Ticket tVentas = new Ticket("TK-VTA", TipoTicket.VENTA, PrioridadTicket.ALTA, "Desc", clienteImss, equipoMedico, LocalDateTime.now().plusDays(1));
        tVentas.setResponsable(ventasResponsable); // Nace ABIERTO por defecto

        entityManager.persist(tTecnico);
        entityManager.persist(tVentas);
        entityManager.flush();

        // Act
        DashboardStatsDTO statsTecnico = ticketRepository.obtenerEstadisticasDashboard(tecnicoResponsable.getId(), EstadoTicket.ABIERTO, EstadoTicket.EN_PROCESO, EstadoTicket.CERRADO);

        // Assert
        assertThat(statsTecnico.abiertos()).isEqualTo(1L);
    }

    @Test
    @DisplayName("findVencidosPorFiltro debe aislar de forma segura los tickets expirados de acuerdo al tipo funcional")
    void findVencidosPorFiltro_filtradoPorTipo_debeGarantizarPoliticaDeVisibilidad() {
        // Arrange
        Ticket vencidoVenta = new Ticket("TK-V-VTA", TipoTicket.VENTA, PrioridadTicket.ALTA, "Desc", clienteImss, equipoMedico, LocalDateTime.now().plusDays(1));
        vencidoVenta.setResponsable(ventasResponsable);

        Ticket vencidoServicio = new Ticket("TK-V-SER", TipoTicket.SERVICIO, PrioridadTicket.ALTA, "Desc", clienteImss, equipoMedico, LocalDateTime.now().plusDays(1));
        vencidoServicio.setResponsable(tecnicoResponsable);

        entityManager.persist(vencidoVenta);
        entityManager.persist(vencidoServicio);
        entityManager.flush();

        // Forzamos el vencimiento de ambos en BD burlando las validaciones de inicialización de la JVM
        entityManager.getEntityManager()
                .createQuery("UPDATE Ticket t SET t.fechaVencimientoSla = :pasado WHERE t.folio IN ('TK-V-VTA', 'TK-V-SER')")
                .setParameter("pasado", LocalDateTime.now().minusDays(1))
                .executeUpdate();

        entityManager.flush();
        entityManager.clear();

        // Act
        Page<Ticket> resultado = ticketRepository.findVencidosPorFiltro(null, TipoTicket.SERVICIO, defaultPageable);

        // Assert
        assertThat(resultado.getContent())
                .hasSize(1)
                .extracting(Ticket::getFolio)
                .containsExactly("TK-V-SER");
    }
}