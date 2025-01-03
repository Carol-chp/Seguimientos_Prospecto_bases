package com.pe.swcotoschero.prospectos.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Builder
@Getter
@Setter
public class ProspectoDTO implements Serializable {
    private Long id;
    private String nombre;
    private String apellido;
    private String celular;
    private String documentoIdentidad;
    private String sexo;
    private String cargo;
    private String distrito;
    private String campania;
    private String subcampania;
}
