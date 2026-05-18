package com.pe.swcotoschero.prospectos.Entity.enums;

/**
 * Estado del ciclo de gestion (= una Asignacion).
 * El estado vive a nivel de CICLO, no de prospecto.
 * Un prospecto puede tener multiples ciclos en el tiempo (un GANADO historico + uno activo).
 */
public enum EstadoGestion {

    /** Cargado por Excel, aun sin asignar a ningun colaborador. */
    DISPONIBLE,

    /** Asignado al colaborador, pero no lo ha trabajado todavia. */
    SIN_GESTIONAR,

    /** El colaborador lo trabaja activamente (llego via resultado INTERESADO). */
    EN_GESTION,

    /** En espera con fecha de recontacto (agendado, reintento NO_CONTESTO, observado SBS). */
    EN_SEGUIMIENTO,

    /** El colaborador lo derivo al dueno para cierre de venta. Solo lectura para el colaborador. */
    DERIVADO,

    /** Venta concretada - la marca el DUENO, no el colaborador. Estado inmutable. */
    GANADO,

    /** Cerrado definitivo: no_volver_llamar, datos invalidos, o ilocalizable (max intentos). */
    DESCARTADO
}
