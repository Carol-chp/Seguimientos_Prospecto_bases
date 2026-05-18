package com.pe.swcotoschero.prospectos.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/** Resultado de leer/validar un Excel: filas válidas + filas rechazadas con motivo. */
@Getter
@Setter
public class LecturaExcelResultado {
    private List<ProspectoDTO> validos = new ArrayList<>();
    private List<FilaRechazada> rechazadas = new ArrayList<>();
}
