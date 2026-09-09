# MesaControl

Sistema de mesa de servicio para la gestión de tickets de soporte técnico y ventas de equipo médico. Permite a hospitales y unidades médicas dar seguimiento a solicitudes de servicio, controlar SLAs y asignar responsables, con control de acceso por rol (Administrador, Ventas, Técnico).

Aplicación web monolítica construida con Spring Boot, pensada para operar como sistema interno de una empresa de servicio técnico biomédico.

## Funcionalidades principales

- **Gestión de tickets**: creación, listado paginado y filtrado, detalle y ciclo de vida controlado (`ABIERTO` → `EN_PROCESO` → `CERRADO`), con transiciones de estado validadas en el dominio.
- **Control de SLA**: cada ticket registra su fecha de vencimiento de SLA; el sistema calcula si está vencido, cuánto tiempo resta y si se cumpliría al cerrarse en ese momento.
- **Asignación de responsables**: un ticket puede asignarse a un usuario técnico, lo que además dispara automáticamente su transición a `EN_PROCESO`.
- **Comentarios y seguimiento**: histórico de comentarios asociado a cada ticket.
- **Gestión de usuarios**: alta, edición y administración de cuentas, restringida a administradores.
- **Dashboard**: panel con indicadores agregados (tickets abiertos, en proceso, cerrados y vencidos).
- **Autenticación y autorización**: login basado en formulario con Spring Security, contraseñas cifradas con BCrypt, control de sesiones concurrentes (una sesión activa por usuario) y autorización por rol a nivel de método (`@PreAuthorize`).
- **Interfaz dinámica sin SPA**: vistas renderizadas del lado del servidor con Thymeleaf, con actualizaciones parciales vía HTMX (evita recargas completas de página).

## Stack técnico

| Capa | Tecnología |
|---|---|
| Lenguaje / runtime | Java 21 |
| Framework | Spring Boot 3.5 |
| Persistencia | Spring Data JPA + PostgreSQL |
| Migraciones de esquema | Flyway |
| Seguridad | Spring Security (form login, BCrypt, sesiones, `@PreAuthorize`) |
| Vistas | Thymeleaf + HTMX |
| Validación | Bean Validation (Jakarta), grupos `OnCreate` / `OnUpdate` |
| Pruebas | JUnit 5, Spring Boot Test, Spring Security Test (unitarias, de repositorio, de integración y de seguridad) |
| CI/CD | GitHub Actions → build, pruebas contra PostgreSQL efímero, despliegue a Azure Web App |

## Arquitectura del proyecto

```
src/main/java/com/biometec/mesacontrol/
├── controller/     # Controladores MVC (tickets, usuarios, auth, errores)
├── service/        # Lógica de negocio (TicketService, UsuarioService)
├── repository/     # Repositorios Spring Data JPA
├── entity/         # Entidades JPA con reglas de negocio (Ticket, Cliente, Equipo, Usuario, Comentario)
├── dto/            # DTOs de entrada/salida, desacoplados de las entidades
├── mapper/         # Conversión entidad ↔ DTO
├── security/       # UserDetailsService personalizado
├── config/         # Configuración de Spring Security
└── exception/      # Manejo centralizado de excepciones (GlobalExceptionHandler)
```

Las entidades del dominio encapsulan sus propias reglas de negocio (por ejemplo, `Ticket` valida las transiciones de estado y calcula el estado del SLA), en lugar de dejar esa lógica dispersa en los servicios.

## Cómo correrlo localmente

1. Levantar la base de datos:

```bash
docker compose up -d
```

2. Definir las variables de conexión (o exportarlas en tu shell):

```bash
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/mesacontrol_db
export SPRING_DATASOURCE_USERNAME=admin_biometec
export SPRING_DATASOURCE_PASSWORD=admin_password
```

3. Ejecutar la aplicación (Flyway aplica las migraciones automáticamente al arrancar):

```bash
./mvnw spring-boot:run
```

4. Abrir `http://localhost:8080/login`.

## Pruebas

El proyecto incluye pruebas unitarias, de repositorio, de integración (MockMvc) y de seguridad:

```bash
./mvnw test
```

En CI, las pruebas de integración corren contra un contenedor efímero de PostgreSQL (ver `.github/workflows/main.yml`).

## CI/CD

El pipeline de GitHub Actions compila el proyecto, ejecuta la suite de pruebas contra PostgreSQL y, en la rama `main`, despliega el artefacto `.jar` a Azure Web App.

## Contexto

Este proyecto fue desarrollado para una empresa de servicio técnico y venta de equipo médico, como sistema interno para el control de tickets de soporte y ventas hacia sus clientes institucionales (hospitales y unidades médicas).
