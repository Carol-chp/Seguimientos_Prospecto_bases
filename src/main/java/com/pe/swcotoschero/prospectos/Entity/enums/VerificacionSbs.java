package com.pe.swcotoschero.prospectos.Entity.enums;

/**
 * Resultado de la verificacion en el portal SBS (Superintendencia de Banca y Seguros).
 * Binario por decision de diseno: APTO u OBSERVADO.
 * La clasificacion detallada del banco queda fuera del alcance (sin integracion API con SBS).
 */
public enum VerificacionSbs {

    /** No presenta observaciones en SBS. Se habilita el wizard de llamada. */
    APTO,

    /** Presenta observaciones. El caso pasa a EN_SEGUIMIENTO hasta la fecha de re-evaluacion. */
    OBSERVADO
}
