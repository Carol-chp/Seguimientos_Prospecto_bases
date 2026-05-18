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
    private long porCerrar;
    private List<BaseResumenDTO> bases;
}
