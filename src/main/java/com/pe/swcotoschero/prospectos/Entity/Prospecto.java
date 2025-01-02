package com.pe.swcotoschero.prospectos.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "prospecto")
public class Prospecto {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long prospectoID;

    private String nombre;
    private String apellido;
    private String celular;

    @Column(name = "documentoidentidad")
    private String documentoIdentidad; // Corregido el nombre del campo

    private String sexo;
    private String banco;
    private String cargo;
    private String distrito;

    @Column(name = "campania") // Ajuste para evitar problemas con tildes
    private String campania;

    @Column(name = "subcampania") // Ajuste para evitar problemas con tildes
    private String subcampania;

    private String comentario;

    private String personal;

    // Getters y Setters

    public Long getProspectoID() {
        return prospectoID;
    }

    public void setProspectoID(Long prospectoID) {
        this.prospectoID = prospectoID;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public void setApellidos(String apellido) {
        this.apellido = apellido;
    }

    public String getCelular() {
        return celular;
    }

    public void setCelular(String celular) {
        this.celular = celular;
    }

    public String getDocumentoIdentidad() {
        return documentoIdentidad;
    }

    public void setDocumentoIdentidad(String documentoIdentidad) {
        this.documentoIdentidad = documentoIdentidad;
    }

    public String getSexo() {
        return sexo;
    }

    public void setSexo(String sexo) {
        this.sexo = sexo;
    }

    public String getBanco() {
        return banco;
    }

    public void setBanco(String banco) {
        this.banco = banco;
    }

    public String getCargo() {
        return cargo;
    }

    public void setCargo(String cargo) {
        this.cargo = cargo;
    }

    public String getDistrito() {
        return distrito;
    }

    public void setDistrito(String distrito) {
        this.distrito = distrito;
    }

    public String getCampania() {
        return campania;
    }

    public void setCampania(String campania) {
        this.campania = campania;
    }

    public String getSubcampania() {
        return subcampania;
    }

    public void setSubcampania(String subcampania) {
        this.subcampania = subcampania;
    }

    public String getComentario() {
        return comentario;
    }

    public void setComentario(String comentario) {
        this.comentario = comentario;
    }

    public String getPersonal() {
        return comentario;
    }

    public void setPersonal(String personal) {
        this.personal = personal;
    }
}
