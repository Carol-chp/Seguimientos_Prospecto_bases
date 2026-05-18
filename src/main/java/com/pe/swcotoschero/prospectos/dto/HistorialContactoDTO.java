package com.pe.swcotoschero.prospectos.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Entrada del historial de contactos de un prospecto (GET /api/contactos/historial/{id}).
 * Cubre atenciones de llamada y de verificacion SBS en todos los ciclos del prospecto.
 */
@Getter
@AllArgsConstructor
public class HistorialContactoDTO {
    private String fechaContacto;
    private String resultado;
    private String submotivoNoContesto;
    private String quienContesto;
    private String verificacionSbs;
    private String comentario;
    private Integer duracionGestion;
}
