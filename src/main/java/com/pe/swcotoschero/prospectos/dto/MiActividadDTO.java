package com.pe.swcotoschero.prospectos.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO del endpoint GET /api/asignaciones/mi-actividad (RF-17).
 *
 * Agrupa los contactos registrados HOY por el colaborador autenticado
 * junto con un resumen de conteos por resultado.
 */
@Getter
@Setter
@Builder
public class MiActividadDTO {

    /** Fecha/hora del momento en que se generó la respuesta (zona America/Lima). */
    private LocalDateTime generadoEn;

    /** Total de gestiones (contactos) registradas hoy por este colaborador. */
    private int totalGestiones;

    /**
     * Conteo por estadoResultado.
     * Clave = nombre del enum ResultadoAtencion (ej. "NO_CONTESTO", "AGENDADO").
     * Valor = cantidad de veces que ese resultado se registró hoy.
     */
    private Map<String, Long> resumenPorResultado;

    /** Detalle de cada gestión registrada hoy, orden cronológico descendente. */
    private List<ItemActividadDTO> gestiones;

    // -------------------------------------------------------------------------
    // Item (gestión individual)
    // -------------------------------------------------------------------------

    @Getter
    @Setter
    @Builder
    public static class ItemActividadDTO {
        private Long contactoId;
        private Long asignacionId;
        private Long prospectoId;
        /** Nombre + apellido del prospecto. */
        private String nombreProspecto;
        /** Celular enmascarado (últimos 3 dígitos). */
        private String celular;
        private LocalDateTime fechaContacto;
        /** Nombre del enum ResultadoAtencion (null si el evento es solo SBS). */
        private String estadoResultado;
        /** Nombre del enum VerificacionSbs (p. ej. OBSERVADO) para eventos SBS sin llamada. */
        private String verificacionSbs;
        private String comentario;
        /** Duración de la gestión en segundos (null si no se registró). */
        private Integer duracionGestion;
    }
}
