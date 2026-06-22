package com.biometec.mesacontrol.entity;

/**
 * Enumeración que define los niveles de prioridad de un ticket.
 *
 * La prioridad determina la urgencia y el orden de procesamiento:
 * - BAJA (3): Problemas menores, puede esperar
 * - MEDIA (2): Problemas moderados, procesamiento estándar
 * - ALTA (1): Problemas críticos, requiere atención inmediata
 *
 * El valor numérico indica el nivel de prioridad (menor número = mayor prioridad)
 *
 * @author Juan Jaramillo
 * @version 1.0
 */
public enum PrioridadTicket {

    /**
     * Prioridad ALTA.
     * Problemas críticos que requieren atención inmediata.
     * El equipo está fuera de servicio o hay peligro inminente.
     */
    ALTA(1, "Alta", "#FF0000"),

    /**
     * Prioridad MEDIA.
     * Problemas moderados con impacto significativo.
     * Requiere atención en el turno actual pero sin urgencia extrema.
     */
    MEDIA(2, "Media", "#FFA500"),

    /**
     * Prioridad BAJA.
     * Problemas menores con bajo impacto.
     * Puede ser procesado en orden de llegada o cuando haya disponibilidad.
     */
    BAJA(3, "Baja", "#00AA00");

    private final Integer nivel;
    private final String descripcion;
    private final String color;

    /**
     * Constructor del enum.
     *
     * @param nivel nivel numérico de prioridad (1=ALTA, 2=MEDIA, 3=BAJA)
     * @param descripcion descripción legible de la prioridad
     * @param color código hexadecimal para representación visual en UI
     */
    PrioridadTicket(Integer nivel, String descripcion, String color) {
        this.nivel = nivel;
        this.descripcion = descripcion;
        this.color = color;
    }

    /**
     * Obtiene el nivel numérico de prioridad.
     * Menor número significa mayor prioridad.
     *
     * @return nivel numérico de prioridad
     */
    public Integer getNivel() {
        return nivel;
    }

    /**
     * Obtiene la descripción legible de la prioridad.
     *
     * @return descripción de la prioridad
     */
    public String getDescripcion() {
        return descripcion;
    }

    /**
     * Obtiene el código de color para representación visual.
     * Útil para codificar por colores en la interfaz de usuario.
     *
     * @return código hexadecimal del color
     */
    public String getColor() {
        return color;
    }

    /**
     * Obtiene el nombre del enum en formato legible para UI.
     *
     * @return nombre de la prioridad
     */
    public String getNombre() {
        return this.name();
    }

    /**
     * Compara dos prioridades.
     * Retorna true si esta prioridad es superior (más urgente) que la otra.
     *
     * @param otra la otra prioridad a comparar
     * @return true si esta prioridad es más urgente, false en caso contrario
     */
    public boolean esMasUrgentQue(PrioridadTicket otra) {
        if (otra == null) {
            return true;
        }
        return this.nivel < otra.nivel;
    }
}