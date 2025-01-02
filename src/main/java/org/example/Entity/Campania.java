package org.example.Entity;

import javax.persistence.*;

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
