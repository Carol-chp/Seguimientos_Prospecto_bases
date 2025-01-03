package com.pe.swcotoschero.prospectos.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class ProspectoBusquedaResponseDTO {

    private Long total;
    private Integer pagina;
    private Integer tamanioPagina;
    private List<ProspectoDTO> resultados;
}
