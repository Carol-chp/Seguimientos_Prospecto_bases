package com.pe.swcotoschero.prospectos.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Reparto de una carga a varios colaboradores en un solo flujo (RF-19).
 * Solo cantidad exacta (decidido). La suma de cantidades no puede exceder
 * los prospectos sin asignar de la carga.
 */
@Getter
@Setter
public class AsignacionMultiRequest {

    private Long cargaMasivaId;
    private List<Item> asignaciones;

    @Getter
    @Setter
    public static class Item {
        private Long usuarioId;
        private Integer cantidad;
    }
}
