package com.pe.swcotoschero.prospectos.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/** Una fila del Excel que no se pudo importar, con el motivo. */
@Getter
@Setter
@AllArgsConstructor
public class FilaRechazada {
    /** Número de fila en el Excel (1-based, como lo ve el usuario). */
    private int fila;
    private String motivo;
}
