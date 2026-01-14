package com.pe.swcotoschero.prospectos.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ContactoRegistroDTO {
    private Long prospectoId;
    private String comentario;
    private Boolean contestoLlamada;
    private Boolean interesado;
}