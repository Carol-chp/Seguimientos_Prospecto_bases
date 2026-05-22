package com.pe.swcotoschero.prospectos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload para crear o actualizar un Banco.
 * En PUT también se aceptan activo y esDefault.
 */
@Data
@NoArgsConstructor
public class BancoRequestDTO {

    @NotBlank(message = "El nombre del banco es obligatorio")
    @Size(max = 80, message = "El nombre no puede superar los 80 caracteres")
    private String nombre;

    /** Activo/inactivo. En POST se ignora (siempre true). */
    private Boolean activo;

    /**
     * Si true, este banco pasa a ser el default para nuevas importaciones.
     * El servicio desmarca el anterior default automáticamente.
     */
    private Boolean esDefault;

    /** ID del banco destino al que se envían los OBSERVADO. Null para limpiar. */
    private Long bancoDestinoId;
}
