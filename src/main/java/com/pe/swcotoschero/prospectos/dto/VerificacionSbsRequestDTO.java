package com.pe.swcotoschero.prospectos.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * Request del Paso 0 — verificacion SBS (RF-15).
 * resultado: "APTO" | "OBSERVADO"
 * fechaReevaluacion: "YYYY-MM-DD" (opcional; solo se usa cuando resultado=OBSERVADO)
 * comentario: texto libre (opcional)
 */
@Getter
@Setter
public class VerificacionSbsRequestDTO {
    private Long prospectoId;
    private String resultado;
    private String fechaReevaluacion;
    private String comentario;
}
