package com.pe.swcotoschero.prospectos.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ProspectoBusquedaRequestDTO {
    private String campania;
    private String textoBusqueda;
    private Integer pagina;
    private Integer tamanioPagina;
}
