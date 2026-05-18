package com.pe.swcotoschero.prospectos.Entity.enums;

/**
 * Los 11 filtros de la cola del colaborador (RF-17).
 * Cada valor define exactamente qué asignaciones se incluyen — ver AsignacionRepository
 * para la query JPQL correspondiente a cada filtro.
 */
public enum FiltroColaborador {

    /**
     * Vista por defecto: todo lo accionable HOY.
     * Incluye SIN_GESTIONAR, EN_GESTION y EN_SEGUIMIENTO con fechaAgenda <= fin de hoy.
     * Los EN_SEGUIMIENTO con fechaAgenda futura también se incluyen (al final) para que
     * el colaborador los vea pero atenuados.
     */
    MI_COLA_HOY,

    /** Solo asignaciones sin gestionar (nunca tocadas). */
    SIN_GESTIONAR,

    /** EN_SEGUIMIENTO + estadoResultado=AGENDADO + fechaAgenda dentro del día de hoy. */
    AGENDADOS_HOY,

    /** EN_SEGUIMIENTO + estadoResultado=NO_CONTESTO + fechaAgenda <= ahora (reintentos vencidos). */
    POR_REINTENTAR,

    /** Todos los EN_SEGUIMIENTO (incluye futuros). */
    PROGRAMADOS,

    /** EN_SEGUIMIENTO + verificacionSbs=OBSERVADO (forward-compatible con slice 1.3). */
    OBSERVADO_SBS,

    /** Estado DERIVADO — solo lectura para el colaborador. */
    DERIVADOS,

    /** EN_GESTION + estadoResultado=INTERESADO. */
    INTERESADOS,

    /** Estado GANADO — ventas del colaborador. */
    MIS_VENTAS,

    /** Estado DESCARTADO — casos cerrados definitivamente. */
    DESCARTADOS,

    /** Todas las asignaciones del colaborador (cualquier estado). */
    TODOS
}
