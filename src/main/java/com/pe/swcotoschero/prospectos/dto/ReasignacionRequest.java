package com.pe.swcotoschero.prospectos.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/** Reasignación manual (RF-23). asignacionIds solo para reasignar selección. */
@Getter
@Setter
public class ReasignacionRequest {
    private List<Long> asignacionIds;
    private Long nuevoUsuarioId;
    private String motivo;
}
