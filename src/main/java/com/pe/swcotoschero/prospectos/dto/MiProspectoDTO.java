package com.pe.swcotoschero.prospectos.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class MiProspectoDTO {
    private Long prospectoId;
    private String nombre;
    private String apellido;
    private String celular;
    private String documentoIdentidad;
    private String campania;
    private String estado; // SIN_GESTIONAR, EN_GESTION, FINALIZADO
    private String estadoResultado; // NO_CONTESTO, AGENDADO, PROSPECTO, OBSERVADO, CONCRETO_PRESTAMO, NO_VOLVER_LLAMAR
    private LocalDateTime fechaAgenda;
    private LocalDateTime ultimoContacto;
    private int totalContactos;
}
