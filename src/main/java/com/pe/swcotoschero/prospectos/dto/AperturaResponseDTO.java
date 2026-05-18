package com.pe.swcotoschero.prospectos.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Respuesta del endpoint POST /api/contactos/apertura (RF-14).
 */
@Getter
@AllArgsConstructor
public class AperturaResponseDTO {
    private Long aperturaId;
    private String inicio;
}
