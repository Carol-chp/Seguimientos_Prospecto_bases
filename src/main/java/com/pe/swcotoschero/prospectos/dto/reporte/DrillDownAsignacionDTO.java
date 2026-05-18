package com.pe.swcotoschero.prospectos.dto.reporte;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Item del drill-down de asignaciones de un colaborador.
 * GET /api/reportes/colaborador/{usuarioId}
 */
@Getter
@Builder
public class DrillDownAsignacionDTO {

    private Long asignacionId;
    private Long prospectoId;

    // Prospecto (enmascarado — celular/doc ultimos 3)
    private String nombreProspecto;
    private String celular;
    private String documentoIdentidad;
    private String campania;

    // Estado del ciclo
    private String estado;
    private String estadoResultado;
    private LocalDateTime fechaAgenda;
    private LocalDateTime fechaAsignacion;
    private LocalDateTime fechaCierre;

    private long totalContactos;
}
