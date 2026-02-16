package com.pe.swcotoschero.prospectos.dto;

import java.time.LocalDateTime;
import java.util.List;

public class CargaMasivaDTO {
    private Long id;
    private String nombrearchivo;
    private LocalDateTime fecha;
    private Integer cantidadProspectos;
    private String estadoAsignacion;
    private LocalDateTime fechaAsignacion;
    
    // Información de asignación parcial
    private int prospectosAsignados;
    private int prospectosSinAsignar;
    private List<AsignacionResumenDTO> resumenAsignaciones;

    // Información del usuario asignado
    private Long usuarioAsignadoId;
    private String usuarioAsignadoNombre;
    private String usuarioAsignadoApellidos;
    private String usuarioAsignadoCompleto;

    // Constructor vacío
    public CargaMasivaDTO() {}

    // Constructor completo
    public CargaMasivaDTO(Long id, String nombrearchivo, LocalDateTime fecha, 
                         Integer cantidadProspectos, String estadoAsignacion, 
                         LocalDateTime fechaAsignacion, Long usuarioAsignadoId, 
                         String usuarioAsignadoNombre, String usuarioAsignadoApellidos) {
        this.id = id;
        this.nombrearchivo = nombrearchivo;
        this.fecha = fecha;
        this.cantidadProspectos = cantidadProspectos;
        this.estadoAsignacion = estadoAsignacion;
        this.fechaAsignacion = fechaAsignacion;
        this.usuarioAsignadoId = usuarioAsignadoId;
        this.usuarioAsignadoNombre = usuarioAsignadoNombre;
        this.usuarioAsignadoApellidos = usuarioAsignadoApellidos;
        
        // Crear nombre completo
        if (usuarioAsignadoNombre != null && usuarioAsignadoApellidos != null) {
            this.usuarioAsignadoCompleto = usuarioAsignadoNombre + " " + usuarioAsignadoApellidos;
        }
    }

    // Getters y Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombrearchivo() {
        return nombrearchivo;
    }

    public void setNombrearchivo(String nombrearchivo) {
        this.nombrearchivo = nombrearchivo;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public Integer getCantidadProspectos() {
        return cantidadProspectos;
    }

    public void setCantidadProspectos(Integer cantidadProspectos) {
        this.cantidadProspectos = cantidadProspectos;
    }

    public String getEstadoAsignacion() {
        return estadoAsignacion;
    }

    public void setEstadoAsignacion(String estadoAsignacion) {
        this.estadoAsignacion = estadoAsignacion;
    }

    public LocalDateTime getFechaAsignacion() {
        return fechaAsignacion;
    }

    public void setFechaAsignacion(LocalDateTime fechaAsignacion) {
        this.fechaAsignacion = fechaAsignacion;
    }

    public Long getUsuarioAsignadoId() {
        return usuarioAsignadoId;
    }

    public void setUsuarioAsignadoId(Long usuarioAsignadoId) {
        this.usuarioAsignadoId = usuarioAsignadoId;
    }

    public String getUsuarioAsignadoNombre() {
        return usuarioAsignadoNombre;
    }

    public void setUsuarioAsignadoNombre(String usuarioAsignadoNombre) {
        this.usuarioAsignadoNombre = usuarioAsignadoNombre;
        updateUsuarioCompleto();
    }

    public String getUsuarioAsignadoApellidos() {
        return usuarioAsignadoApellidos;
    }

    public void setUsuarioAsignadoApellidos(String usuarioAsignadoApellidos) {
        this.usuarioAsignadoApellidos = usuarioAsignadoApellidos;
        updateUsuarioCompleto();
    }

    public String getUsuarioAsignadoCompleto() {
        return usuarioAsignadoCompleto;
    }

    public void setUsuarioAsignadoCompleto(String usuarioAsignadoCompleto) {
        this.usuarioAsignadoCompleto = usuarioAsignadoCompleto;
    }

    private void updateUsuarioCompleto() {
        if (usuarioAsignadoNombre != null && usuarioAsignadoApellidos != null) {
            this.usuarioAsignadoCompleto = usuarioAsignadoNombre + " " + usuarioAsignadoApellidos;
        }
    }

    public int getProspectosAsignados() {
        return prospectosAsignados;
    }

    public void setProspectosAsignados(int prospectosAsignados) {
        this.prospectosAsignados = prospectosAsignados;
    }

    public int getProspectosSinAsignar() {
        return prospectosSinAsignar;
    }

    public void setProspectosSinAsignar(int prospectosSinAsignar) {
        this.prospectosSinAsignar = prospectosSinAsignar;
    }

    public List<AsignacionResumenDTO> getResumenAsignaciones() {
        return resumenAsignaciones;
    }

    public void setResumenAsignaciones(List<AsignacionResumenDTO> resumenAsignaciones) {
        this.resumenAsignaciones = resumenAsignaciones;
    }
}