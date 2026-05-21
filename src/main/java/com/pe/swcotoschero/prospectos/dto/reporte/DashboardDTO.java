package com.pe.swcotoschero.prospectos.dto.reporte;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Respuesta del endpoint GET /api/reportes/dashboard.
 * Contrato exacto definido en §5e del MVP (RF-18).
 */
@Getter
@Builder
public class DashboardDTO {

    private MetricasPeriodoDTO dia;
    private MetricasPeriodoDTO mes;
    private List<RankingColaboradorDTO> ranking;
    private EmbudoDTO embudo;
    private List<BaseResumenDTO> bases;

    /**
     * Asistencia del día (RF-22 / 2.4): {fecha, esLaborable, totalColaboradores,
     * totalAusentes, colaboradores:[{usuarioId,nombre,jornadaIniciada,inicio,fin,ausente}]}.
     */
    private Object asistencia;

    /** Casos "En riesgo" (5j / 2.3): de colaboradores ausentes hoy, por reasignar. */
    private long porEnRiesgo;
}
