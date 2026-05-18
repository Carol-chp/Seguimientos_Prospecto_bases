package com.pe.swcotoschero.prospectos.dto.reporte;

import lombok.Builder;
import lombok.Getter;

/**
 * Resumen de avance de una carga masiva (base de prospectos).
 * RF-18 §5f.
 */
@Getter
@Builder
public class BaseResumenDTO {

    private Long id;
    private String nombre;
    private long cantidad;
    private long asignados;
    private long sinAsignar;
    private double avancePct;
}
