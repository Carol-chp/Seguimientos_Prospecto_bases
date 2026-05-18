package com.pe.swcotoschero.prospectos.dto.reporte;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Una fila del ranking de colaboradores (metricas del mes).
 * RF-18 §5e.
 */
@Getter
@Builder
public class RankingColaboradorDTO {

    private Long usuarioId;
    private String nombre;
    private long ventasCerradas;
    private long derivados;
    private long atenciones;
    private double contactabilidad;
    private LocalDateTime ultimaActividad;
}
