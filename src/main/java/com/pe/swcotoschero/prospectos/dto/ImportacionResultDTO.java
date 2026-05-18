package com.pe.swcotoschero.prospectos.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/** Respuesta de la importación de Excel: cuántos entraron y qué filas se rechazaron. */
@Getter
@Setter
@Builder
public class ImportacionResultDTO {
    private boolean success;
    private String mensaje;
    private Long cargaMasivaId;
    private int importados;
    private int rechazados;
    private List<FilaRechazada> detalleRechazos;
}
