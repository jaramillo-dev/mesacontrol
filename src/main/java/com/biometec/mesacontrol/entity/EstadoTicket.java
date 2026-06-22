package com.biometec.mesacontrol.entity;

/**
 * Enumeración que define los estados posibles de un ticket.
 *
 * Los estados representan el ciclo de vida de un ticket:
 * - ABIERTO: Ticket recientemente creado, pendiente de procesamiento
 * - EN_PROCESO: Ticket en seguimiento activo y siendo trabajado
 * - CERRADO: Ticket finalizado, ya no requiere atención
 *
 * El flujo natural es: ABIERTO → EN_PROCESO → CERRADO
 *
 * @author Juan Jaramillo
 * @version 1.0
 */
public enum EstadoTicket {

    /**
     * Estado inicial del ticket.
     * Indica que el ticket ha sido creado pero aún no ha comenzado el procesamiento.
     */
    ABIERTO("Abierto", 0),

    /**
     * Estado intermedio del ticket.
     * Indica que el ticket está siendo procesado activamente.
     */
    EN_PROCESO("En Proceso", 1),

    /**
     * Estado final del ticket.
     * Indica que el ticket ha sido completado y cabe la posibilidad de cierre.
     * La fecha_cierre debe estar establecida.
     */
    CERRADO("Cerrado", 2);

    private final String descripcion;
    private final Integer orden;

    /**
     * Constructor del enum.
     *
     * @param descripcion descripción legible del estado
     * @param orden orden secuencial del estado en el flujo
     */
    EstadoTicket(String descripcion, Integer orden) {
        this.descripcion = descripcion;
        this.orden = orden;
    }

    /**
     * Obtiene la descripción legible del estado del ticket.
     *
     * @return descripción del estado
     */
    public String getDescripcion() {
        return descripcion;
    }

    /**
     * Obtiene el orden secuencial del estado en el flujo de vida del ticket.
     *
     * @return orden del estado
     */
    public Integer getOrden() {
        return orden;
    }

    /**
     * Obtiene el nombre del enum en formato legible para UI.
     *
     * @return nombre del estado
     */
    public String getNombre() {
        return this.name();
    }

    /**
     * Verifica si es válida la transición de estados.
     * Solo se permite avanzar en el flujo: ABIERTO → EN_PROCESO → CERRADO
     *
     * @param nuevoEstado el estado hacia el cual se desea transicionar
     * @return true si la transición es válida, false en caso contrario
     */
    public boolean esTransicionValida(EstadoTicket nuevoEstado) {
        if (nuevoEstado == null) {
            return false;
        }
        // No se permite transicionar hacia el mismo estado
        if (this == nuevoEstado) {
            return false;
        }
        // Solo se permite avanzar en el orden
        return nuevoEstado.orden > this.orden;
    }
}