package com.pe.swcotoschero.prospectos.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO de proyección del ciclo (Asignacion) para la cola del colaborador (RF-17).
 *
 * Enmascarado de datos sensibles (RF-13):
 *  - celular: solo últimos 3 dígitos visibles ("*****789"). celularMasked=true siempre.
 *  - documentoIdentidad: solo últimos 3 dígitos visibles ("*****456").
 * La búsqueda server-side opera sobre los valores reales; la respuesta va enmascarada.
 *
 * Banderas de posición en MI_COLA_HOY:
 *  - vencido: EN_SEGUIMIENTO y fechaAgenda < ahora (debería haberse contactado ya).
 *  - futuro:  EN_SEGUIMIENTO y fechaAgenda > fin de hoy (programado para un día posterior).
 * Ambas son false para estados distintos de EN_SEGUIMIENTO.
 */
@Getter
@Setter
@Builder
public class MiProspectoDTO {

    // --- Identificadores ---
    private Long asignacionId;
    private Long prospectoId;

    // --- Datos del prospecto (enmascarados) ---
    private String nombre;
    private String apellido;
    /** Celular con los primeros dígitos reemplazados por asteriscos (ej. "*****789"). */
    private String celular;
    /** Siempre true: indica al frontend que el número está enmascarado. */
    private boolean celularMasked;
    /** DNI/documento con los primeros dígitos enmascarados (ej. "*****456"). */
    private String documentoIdentidad;
    private String campania;

    // --- Estado del ciclo ---
    /** Nombre del enum EstadoGestion (ej. "SIN_GESTIONAR", "EN_SEGUIMIENTO"). */
    private String estado;
    /** Nombre del enum ResultadoAtencion de la última atención. Null si aún no se gestionó. */
    private String estadoResultado;

    // --- Fechas ---
    private LocalDateTime fechaAgenda;
    private LocalDateTime ultimoContacto;

    // --- Verificación SBS ---
    /** Nombre del enum VerificacionSbs ("APTO" u "OBSERVADO"). Null si aún no se verificó. */
    private String verificacionSbs;
    private LocalDate fechaReevaluacionSbs;

    // --- Llamada ---
    /** Fecha/hora sugerida para el próximo intento (calculada o editada). */
    private LocalDateTime proximaLlamada;
    /** Número de intentos fallidos (NO_CONTESTO). */
    private int intentosFallidos;

    // --- Contactos ---
    private int totalContactos;

    // --- Badge cliente recurrente ---
    /** nroPrestamosConcretados del prospecto. >0 activa el badge "Cliente recurrente". */
    private int nroPrestamosConcretados;

    // --- Banderas de posición en MI_COLA_HOY ---
    /**
     * true si estado=EN_SEGUIMIENTO y fechaAgenda < ahora.
     * La UI lo resalta como urgente/vencido.
     */
    private boolean vencido;
    /**
     * true si estado=EN_SEGUIMIENTO y fechaAgenda > fin del día de hoy.
     * La UI lo atenúa (todavía no es accionable hoy).
     */
    private boolean futuro;
}
