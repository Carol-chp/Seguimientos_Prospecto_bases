package com.pe.swcotoschero.prospectos.Entity.enums;

/**
 * Detalle de por que no contesto la llamada.
 * Solo aplica cuando ResultadoAtencion = NO_CONTESTO.
 */
public enum SubmotivoNoContesto {

    /** Timbraba y nadie respondio. */
    NO_CONTESTA,

    /** Entro a buzon de voz. */
    BUZON,

    /** Linea ocupada o colgaron. */
    OCUPADO,

    /** Celular apagado o fuera de servicio. */
    APAGADO
}
