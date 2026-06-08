package com.biometec.mesacontrol.config;

import com.biometec.mesacontrol.security.AppUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.session.HttpSessionEventPublisher;

/**
 * Configuración central de Spring Security para la aplicación MesaControl.
 *
 * <p>Esta clase define la cadena de filtros de seguridad, la estrategia de
 * autenticación, el manejo de sesiones y las políticas de autorización por URL.
 * Todas las decisiones de seguridad relevantes están centralizadas aquí.</p>
 *
 * <p>Componentes configurados:</p>
 * <ul>
 *   <li>Autenticación basada en base de datos via {@link AppUserDetailsService}</li>
 *   <li>Cifrado de contraseñas con BCrypt (factor de costo por defecto: 10)</li>
 *   <li>Formulario de login personalizado con redirección post-autenticación</li>
 *   <li>Gestión de sesiones con límite de una sesión concurrente por usuario</li>
 *   <li>Manejo de errores 401 y 403 con redirección a vistas Thymeleaf</li>
 * </ul>
 *
 * <p>La autorización por método ({@code @PreAuthorize}, {@code @Secured}) está
 * habilitada globalmente a través de {@link EnableMethodSecurity}, lo que permite
 * aplicar restricciones por rol directamente en los controladores y servicios
 * sin necesidad de duplicar reglas aquí.</p>
 *
 * @author Juan Jaramillo
 * @version 1.0
 * @see AppUserDetailsService
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final AppUserDetailsService userDetailsService;

    /**
     * Define la cadena de filtros de seguridad principal de la aplicación.
     *
     * <p>El orden de configuración importa: Spring Security evalúa las reglas
     * de autorización de arriba hacia abajo, por lo que las rutas más
     * específicas deben declararse antes que las más generales.</p>
     *
     * @param http constructor de la configuración de seguridad HTTP
     * @return la cadena de filtros construida y lista para usar
     * @throws Exception si alguna configuración del builder falla
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Registra el proveedor de autenticación basado en base de datos.
                // Debe declararse antes de authorizeHttpRequests para que Spring
                // Security lo use al evaluar las credenciales del formulario de login.
                .authenticationProvider(authenticationProvider())

                .authorizeHttpRequests(auth -> auth
                        // Rutas públicas: el login y las vistas de error deben ser
                        // accesibles sin autenticación. Sin el permiso explícito a
                        // /error/**, Spring Security interceptaría las redirecciones
                        // de error y causaría un bucle infinito de redirecciones.
                        .requestMatchers("/","/login", "/error/**").permitAll()
                        // Cualquier otra ruta exige sesión autenticada.
                        // Los permisos por rol se gestionan con @PreAuthorize
                        // en cada controlador gracias a @EnableMethodSecurity.
                        .anyRequest().authenticated()
                )

                .formLogin(form -> form
                        // Ruta del formulario de login personalizado (Thymeleaf).
                        // Spring Security publica automáticamente el endpoint POST
                        // /login para procesar las credenciales enviadas.
                        .loginPage("/login")
                        // Destino tras un login exitoso. El segundo parámetro (true)
                        // fuerza siempre esta URL, ignorando la página que el usuario
                        // intentaba visitar antes de ser redirigido al login.
                        // Cambiar a false si quieres redirigir al destino original.
                        .defaultSuccessUrl("/dashboard", false)
                        .permitAll()
                )

                .logout(logout -> logout
                        // Tras el logout, redirige al login con el parámetro ?logout
                        // que puede usarse en la vista para mostrar un mensaje de confirmación.
                        // Ejemplo en Thymeleaf: th:if="${param.logout}"
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                )

                .sessionManagement(session -> session
                        // IF_REQUIRED: Spring crea la sesión solo cuando es necesario.
                        // Evita crear sesiones vacías para peticiones de recursos estáticos.
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .maximumSessions(1)
                        // false: si el usuario ya tiene una sesión activa, la sesión anterior
                        // se invalida y el nuevo login tiene prioridad.
                        // Cambiar a true para bloquear el segundo login directamente.
                        .maxSessionsPreventsLogin(false)
                        // Ruta a la que se redirige cuando la sesión fue invalidada
                        // por un login concurrente. El parámetro ?expired permite
                        // mostrar un aviso en el formulario de login.
                        .expiredUrl("/login?expired")
                )

                .exceptionHandling(ex -> ex
                        // 401 — Sin autenticación: el usuario no tiene sesión activa
                        // e intenta acceder a un recurso protegido.
                        // Nota: /error/401 debe estar en el permitAll() de arriba;
                        // de lo contrario Spring Security lo interceptaría y
                        // generaría un bucle infinito de redirecciones.
                        .authenticationEntryPoint((request, response, authException) ->
                                response.sendRedirect("/error/401"))

                        // 403 — Acceso denegado: el usuario está autenticado pero
                        // su rol no tiene permiso para el recurso solicitado.
                        // Típicamente lanzado cuando falla un @PreAuthorize.
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                response.sendRedirect("/error/403"))
                );

        return http.build();
    }

    /**
     * Registra el codificador de contraseñas con el algoritmo BCrypt.
     *
     * <p>BCrypt incorpora un salt aleatorio por contraseña y un factor de costo
     * configurable (por defecto 10). Esto lo hace resistente a ataques de
     * fuerza bruta y rainbow tables. Nunca almacena la contraseña en texto plano.</p>
     *
     * <p>Este bean se inyecta en {@link #authenticationProvider()} y en
     * {@code UsuarioService} para cifrar contraseñas al crear o editar usuarios.</p>
     *
     * @return instancia de {@link BCryptPasswordEncoder} con factor de costo 10
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Configura el proveedor de autenticación basado en base de datos.
     *
     * <p>{@link DaoAuthenticationProvider} es el proveedor estándar de Spring
     * Security para autenticación con usuario y contraseña almacenados en una
     * fuente de datos. Delega la carga del usuario a {@link AppUserDetailsService}
     * y la verificación de la contraseña al {@link PasswordEncoder}.</p>
     *
     * <p>El flujo de autenticación es:</p>
     * <ol>
     *   <li>El usuario envía credenciales por el formulario POST /login.</li>
     *   <li>Este proveedor llama a {@code userDetailsService.loadUserByUsername()}.</li>
     *   <li>Compara la contraseña enviada contra el hash almacenado con BCrypt.</li>
     *   <li>Si coincide, establece el contexto de seguridad con el usuario autenticado.</li>
     * </ol>
     *
     * @return proveedor de autenticación configurado con DAO y BCrypt
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * Expone el {@link AuthenticationManager} como bean de Spring.
     *
     * <p>Aunque Spring Security lo gestiona internamente, exponerlo como bean
     * permite inyectarlo en otros componentes que necesiten autenticar
     * programáticamente, como un futuro endpoint de login para una API REST
     * o flujos de autenticación personalizados.</p>
     *
     * @param authenticationConfiguration configuración de autenticación de Spring
     * @return el {@link AuthenticationManager} activo en la aplicación
     * @throws Exception si la configuración no puede resolverse
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    /**
     * Registra el publicador de eventos de sesión HTTP.
     *
     * <p>Este bean es <b>obligatorio</b> para que el control de sesiones
     * concurrentes funcione correctamente. Sin él, Spring Security no recibe
     * notificaciones cuando una sesión expira o se destruye, lo que impide
     * que {@code maximumSessions(1)} invalide sesiones antiguas al detectar
     * un nuevo login del mismo usuario.</p>
     *
     * @return publicador de eventos de ciclo de vida de sesiones HTTP
     */
    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }
}