package com.pe.swcotoschero.prospectos.Entity;


import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "Campania")
public class Campania {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer campañaID;

    private String nombre;

    private String descripcion;

    // Getters y Setters
    // ..

    public String getCampaniaID() {
        return nombre;
    }

    public void setCampañaID(String nombre) {
        this.nombre = nombre;
    }

}
