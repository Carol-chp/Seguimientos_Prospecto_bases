package com.pe.swcotoschero.prospectos.dto;

import lombok.Getter;
import lombok.Setter;

/** El dueño registra la VENTA de un caso DERIVADO. fechaElegibilidad es obligatoria. */
@Getter
@Setter
public class CierreVentaRequest {
    /** "YYYY-MM-DD": fecha a partir de la cual el cliente puede tomar otro préstamo (D7). */
    private String fechaElegibilidad;
    private String comentario;
}
