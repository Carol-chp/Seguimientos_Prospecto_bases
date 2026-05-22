package com.pe.swcotoschero.prospectos.dto;

/**
 * Body de POST /api/contactos/enviar-banco.
 *
 * El colaborador indica el prospecto OBSERVADO que desea reenviar
 * al banco destino configurado en su banco actual.
 */
public class EnviarBancoRequestDTO {

    private Long prospectoId;

    public Long getProspectoId() {
        return prospectoId;
    }

    public void setProspectoId(Long prospectoId) {
        this.prospectoId = prospectoId;
    }
}
