package com.biometec.mesacontrol.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Pruebas unitarias rápidas para la red de seguridad del sistema.
 * Válida que los errores no controlados no expongan código al usuario.
 */
class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        // Configuramos MockMvc de forma aislada (Standalone).
        // Se inyecta el DummyController y el manejador de excepciones real.
        // Esto ejecuta la prueba en milisegundos sin levantar el contexto de Spring.
        mockMvc = MockMvcBuilders.standaloneSetup(new DummyController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // ==========================================
    // EDGE CASE: ERROR 500 INESPERADO
    // ==========================================

    @Test
    @DisplayName("Una excepción genérica no controlada debe mostrar la vista amigable 500")
    void manejarErrorGeneral_ExcepcionInesperada_OcultaStackTrace() throws Exception {

        mockMvc.perform(get("/forzar-falla-catastrofica"))
                .andExpect(status().isInternalServerError()) // Esperamos HTTP 500
                .andExpect(view().name("error/manejador-global"))
                .andExpect(model().attribute("codigo", 500))
                .andExpect(model().attribute("titulo", "Error inesperado"))
                // Validamos que el mensaje es el genérico y no el mensaje real de la excepción
                .andExpect(model().attribute("mensaje", "Ocurrió un error interno. Por favor intentá más tarde."))
                .andExpect(model().attribute("ruta", "/forzar-falla-catastrofica"));
    }

    // ==========================================
    // CONTROLADOR SIMULADO PARA LA PRUEBA
    // ==========================================

    @RestController
    static class DummyController {

        @GetMapping("/forzar-falla-catastrofica")
        public void fallar() throws Exception {
            // Simulamos que el código en producción falla terriblemente
            // (ej. base de datos caída, nulo inesperado, división por cero)
            throw new Exception("Mensaje interno súper secreto que el cliente NUNCA debe ver: NullPointerException en línea 45");
        }
    }
}