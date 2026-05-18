package com.pe.swcotoschero.prospectos.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * DTO para registrar una atencion (POST /api/contactos).
 *
 * Compatibilidad hacia atras:
 *  - estadoResultado es el nombre legacy; resultado es el nombre canonico.
 *    El servicio usa resultado si viene; si no, cae a estadoResultado.
 *  - contestoLlamada e interesado (boolean) son deprecados pero se conservan
 *    para no romper clientes viejos.
 */
@Getter
@Setter
public class ContactoRegistroDTO {

    // --- Identificacion del prospecto ---
    private Long prospectoId;

    // --- Resultado de la atencion ---

    /** Nombre canonico del enum ResultadoAtencion (nuevo campo, preferido). */
    private String resultado;

    /**
     * Alias legacy de resultado (mantenido para compatibilidad).
     * El servicio lee resultado primero; si es null, lee estadoResultado.
     */
    private String estadoResultado;

    // --- Detalle de la llamada ---

    /** Obligatorio cuando resultado = NO_CONTESTO. Nombre del enum SubmotivoNoContesto. */
    private String submotivoNoContesto;

    /** Opcional. Nombre del enum QuienContesto (TITULAR/TERCERO/EQUIVOCADO). */
    private String quienContesto;

    /** Texto libre. */
    private String comentario;

    /**
     * Fecha/hora del proximo recontacto.
     * Obligatorio cuando resultado = AGENDADO o VOLVER_LLAMAR.
     * Formato ISO: "yyyy-MM-dd'T'HH:mm" o "yyyy-MM-dd'T'HH:mm:ss".
     */
    private String fechaAgenda;

    // --- Cronometro (RF-13) ---

    /** ID del AperturaEvento abierto al inicio de la gestion (opcional). */
    private Long aperturaId;

    /** Duracion de la gestion en segundos (opcional; si viene aperturaId se prefiere calcular). */
    private Integer duracionGestionSegundos;

    // --- Campos de compatibilidad deprecated ---

    @Deprecated
    private Boolean contestoLlamada;

    @Deprecated
    private Boolean interesado;
}
