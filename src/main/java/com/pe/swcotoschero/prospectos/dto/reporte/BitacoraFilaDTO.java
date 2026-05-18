package com.pe.swcotoschero.prospectos.dto.reporte;

import lombok.Builder;
import lombok.Getter;

/**
 * Una fila de la bitácora global (RF-20 / §5h).
 * Celular enmascarado (auditoría del dueño; mismo criterio que el resto del sistema).
 */
@Getter
@Builder
public class BitacoraFilaDTO {
    private Long contactoId;
    private Long asignacionId;
    private Long prospectoId;
    private String fecha;            // ISO LocalDateTime
    private String colaborador;
    private String prospecto;
    private String celular;          // enmascarado
    private String campania;
    private String base;             // nombre de archivo de la carga masiva
    private String estadoResultado;
    private String submotivoNoContesto;
    private String quienContesto;
    private String verificacionSbs;
    private Integer duracionGestion; // segundos
    private String comentario;
}
