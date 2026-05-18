package com.pe.swcotoschero.prospectos.dto.reporte;

import lombok.Builder;
import lombok.Getter;

/**
 * Metricas agregadas para un periodo (dia o mes).
 * RF-18 §5e — dashboard del dueno.
 */
@Getter
@Builder
public class MetricasPeriodoDTO {

    private long ventasCerradas;
    private long derivados;
    private long atenciones;

    // Solo en "dia"
    private Double contactabilidadReal;
    private Long colaboradoresActivos;
    private Long colaboradoresTotal;
    private Long citasHoy;

    // Solo en "mes"
    private Double tasaConversion;
    private Double avanceBasesPct;
    private Long disponiblesSinAsignar;
}
