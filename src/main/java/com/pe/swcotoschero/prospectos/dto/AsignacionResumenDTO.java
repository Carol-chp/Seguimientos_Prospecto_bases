package com.pe.swcotoschero.prospectos.dto;

public class AsignacionResumenDTO {
    private Long usuarioId;
    private String usuarioNombre;
    private String usuarioApellidos;
    private String usuarioNombreCompleto;
    private Long cantidadAsignada;

    public AsignacionResumenDTO() {}

    public AsignacionResumenDTO(Long usuarioId, String usuarioNombre, String usuarioApellidos, Long cantidadAsignada) {
        this.usuarioId = usuarioId;
        this.usuarioNombre = usuarioNombre;
        this.usuarioApellidos = usuarioApellidos;
        this.usuarioNombreCompleto = usuarioNombre + " " + usuarioApellidos;
        this.cantidadAsignada = cantidadAsignada;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Long usuarioId) {
        this.usuarioId = usuarioId;
    }

    public String getUsuarioNombre() {
        return usuarioNombre;
    }

    public void setUsuarioNombre(String usuarioNombre) {
        this.usuarioNombre = usuarioNombre;
    }

    public String getUsuarioApellidos() {
        return usuarioApellidos;
    }

    public void setUsuarioApellidos(String usuarioApellidos) {
        this.usuarioApellidos = usuarioApellidos;
    }

    public String getUsuarioNombreCompleto() {
        return usuarioNombreCompleto;
    }

    public void setUsuarioNombreCompleto(String usuarioNombreCompleto) {
        this.usuarioNombreCompleto = usuarioNombreCompleto;
    }

    public Long getCantidadAsignada() {
        return cantidadAsignada;
    }

    public void setCantidadAsignada(Long cantidadAsignada) {
        this.cantidadAsignada = cantidadAsignada;
    }
}
