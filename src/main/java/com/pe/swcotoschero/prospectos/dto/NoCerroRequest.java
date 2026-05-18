package com.pe.swcotoschero.prospectos.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * El dueño NO cerró la venta de un caso DERIVADO.
 *  accion = REINTENTAR  → vuelve a EN_SEGUIMIENTO con `fecha` (obligatoria).
 *  accion = DESCARTAR   → DESCARTADO (NO_VOLVER_LLAMAR).
 */
@Getter
@Setter
public class NoCerroRequest {
    private String accion;       // REINTENTAR | DESCARTAR
    private String fecha;        // "YYYY-MM-ddTHH:mm" — obligatoria si REINTENTAR
    private String comentario;
}
