package com.pe.swcotoschero.prospectos.dto;

import lombok.Getter;
import lombok.Setter;

/** Subida de la tarjeta de WhatsApp de un colaborador (imagen en Base64). */
@Getter
@Setter
public class TarjetaWhatsappRequest {
    /** MIME, ej. "image/png" o "image/jpeg". */
    private String contentType;
    /** Imagen codificada en Base64 (sin el prefijo data:). */
    private String base64;
}
