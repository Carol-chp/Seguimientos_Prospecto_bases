package com.pe.swcotoschero.prospectos.Entity;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "Administrador")
public class Administrador {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer administradorID;

    private String nombre;
    private String apellidos;
    private String email;

    @Column(unique = true, nullable = false)
    private String correo;

    // Getters y Setters
    // ...
    public Integer getAdministradorID() {
        return administradorID;
    }

    public void setAdministradorID(Integer administradorID) {
        this.administradorID = administradorID;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getApellidos() {
        return apellidos;
    }

    public void setApellidos(String apellidos) {
        this.apellidos = apellidos;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

}

