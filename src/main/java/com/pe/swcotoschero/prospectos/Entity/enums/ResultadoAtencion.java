package com.pe.swcotoschero.prospectos.Entity.enums;

/**
 * Resultado de la ultima atencion registrada en un ciclo.
 * Determina el EstadoGestion resultante segun la tabla B de REQUERIMIENTOS.md §5b.
 */
public enum ResultadoAtencion {

    /** No atendio la llamada (ver SubmotivoNoContesto para el detalle). Lleva a EN_SEGUIMIENTO. */
    NO_CONTESTO,

    /** No es buen momento, pide que lo llamen. Lleva a EN_SEGUIMIENTO con fecha. */
    VOLVER_LLAMAR,

    /** Se agendo una cita con fecha y hora. Lleva a EN_SEGUIMIENTO. */
    AGENDADO,

    /** Mostro interes sin cita. Lleva a EN_GESTION. */
    INTERESADO,

    /** Acepto; el colaborador lo deriva al dueno para cierre. Lleva a DERIVADO. */
    DERIVADO,

    /** Rechazo explicito, no volver a llamar. Lleva a DESCARTADO. */
    NO_VOLVER_LLAMAR,

    /** Numero inexistente, equivocado o datos incorrectos. Lleva a DESCARTADO. */
    DATOS_INVALIDOS,

    /** Supero el maximo de intentos sin contestar. Lleva a DESCARTADO. Job nocturno lo aplica. */
    ILOCALIZABLE
}
