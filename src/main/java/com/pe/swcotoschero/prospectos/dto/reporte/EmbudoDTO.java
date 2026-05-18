package com.pe.swcotoschero.prospectos.dto.reporte;

import lombok.Builder;
import lombok.Getter;

/**
 * Embudo de conversion global (sin filtro de fecha).
 * RF-18 §5e.
 */
@Getter
@Builder
public class EmbudoDTO {

    private long asignados;
    private long gestionados;
    private long contactadosTitular;
    private long interesados;
    private long derivados;
    private long ventas;
}
