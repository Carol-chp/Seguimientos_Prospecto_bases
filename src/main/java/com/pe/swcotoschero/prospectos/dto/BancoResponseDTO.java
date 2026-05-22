package com.pe.swcotoschero.prospectos.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Respuesta del catálogo de Bancos.
 * Incluye información del banco destino aplanada (sin anidar el objeto completo)
 * para evitar referencias circulares en la serialización JSON.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BancoResponseDTO {

    private Long id;
    private String nombre;
    private Boolean activo;
    private Boolean esDefault;
    private Long bancoDestinoId;
    private String bancoDestinoNombre;
}
