package com.biometec.mesacontrol.entity;

/**
 * Enumeración que define los tipos de tickets soportados.
 *
 * Los tipos de tickets categorizan la naturaleza del servicio o transacción:
 * - VENTA: Tickets relacionados con procesos de venta
 * - SERVICIO: Tickets relacionados con servicios técnicos o de mantenimiento
 *
 * @author Juan Jaramillo
 * @version 1.0
 */
public enum TipoTicket {

    /**
     * Tipo de ticket para transacciones de venta.
     */
    VENTA("Venta"),

    /**
     * Tipo de ticket para servicios técnicos o de mantenimiento.
     */
    SERVICIO("Servicio");

    private final String descripcion;

    /**
     * Constructor del enum.
     *
     * @param descripcion descripción legible del tipo de ticket
     */
    TipoTicket(String descripcion) {
        this.descripcion = descripcion;
    }

    /**
     * Obtiene la descripción legible del tipo de ticket.
     *
     * @return descripción del tipo de ticket
     */
    public String getDescripcion() {
        return descripcion;
    }

    /**
     * Obtiene el nombre del enum en formato legible para UI.
     *
     * @return nombre del tipo de ticket
     */
    public String getNombre() {
        return this.name();
    }
}