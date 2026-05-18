package com.pe.swcotoschero.prospectos.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** Un caso DERIVADO esperando cierre del dueño (5c.bis). */
@Getter
@Setter
@Builder
public class PorCerrarItemDTO {
    private Long asignacionId;
    private Long prospectoId;
    private String nombre;
    private String apellido;
    private String celular;            // enmascarado
    private boolean celularMasked;
    private String documentoIdentidad; // enmascarado
    private String campania;
    private Long derivadoPorId;
    private String derivadoPorNombre;
    private LocalDateTime fechaDerivacion;
    private int nroPrestamosConcretados; // badge cliente recurrente
}
