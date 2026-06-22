package com.biometec.mesacontrol.dto;


/**
 * DTO inmutable que agrupa los indicadores principales mostrados
 * en el panel de control de la aplicación.
 *
 * <p>
 * Cada propiedad representa el total consolidado de tickets para una
 * categoría específica utilizada por el dashboard.
 * </p>
 * @author Juan Jaramillo
 * @version 1.0
 */
public record DashboardStatsDTO(
        long abiertos,
        long enProceso,
        long cerrados,
        long vencidos
) {}