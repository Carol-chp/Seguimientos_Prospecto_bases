package com.pe.swcotoschero.prospectos.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "Asignacion")

public class Asignacion {
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer asignacionID;

    @ManyToOne
    @JoinColumn(name = "prospecto_id", nullable = false)
    private Prospecto prospecto;

    @ManyToOne
    @JoinColumn(name = "personal_id", nullable = false)
    private Usuario usuario;

    @ManyToOne
    @JoinColumn(name = "administrador_id", nullable = false)
    private Usuario administrador;

    @Column(name = "fecha_asignacion")
    private LocalDateTime fechaAsignacion = LocalDateTime.now();

    @Column(nullable = false)
    private String estado;

    //private String estado = "Pendiente";

    // Constructor por defecto
    public Asignacion() {
        this.fechaAsignacion = LocalDateTime.now(); // Fecha predeterminada al momento de creación
        this.estado = "Pendiente"; // Estado inicial
    }

    // Getters y Setters

    public Integer getAsignacionID() {
        return asignacionID;
    }

    public void setAsignacionID(Integer asignacionID) {
        this.asignacionID = asignacionID;
    }

    public Prospecto getProspecto() {
        return prospecto;
    }

    public void setProspecto(Prospecto prospecto) {
        this.prospecto = prospecto;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public LocalDateTime getFechaAsignacion() {
        return fechaAsignacion;
    }

    public void setFechaAsignacion(LocalDateTime fechaAsignacion) {
        this.fechaAsignacion = fechaAsignacion;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public Usuario getAdministrador() {
        return administrador;
    }

    public void setAdministrador(Usuario administrador) {
        this.administrador = administrador;
    }

}
