package org.example.Entity;

import javax.persistence.*;
import java.time.LocalDateTime;
@Entity
@Table(name = "Contacto")

public class Contacto {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer contactoID;

    @ManyToOne
    @JoinColumn(name = "asignacion_id", nullable = false)
    private Asignacion asignacion;

    @Column(name = "fecha_contacto")
    private LocalDateTime fechaContacto = LocalDateTime.now();

    private String comentario;

    // Getters y Setters
    public Integer getContactoID() {
        return contactoID;
    }

    public void setContactoID(Integer contactoID) {
        this.contactoID = contactoID;
    }

    public Asignacion getAsignacion() {
        return asignacion;
    }

    public void setAsignacion(Asignacion asignacion) {
        this.asignacion = asignacion;
    }

    public LocalDateTime getFechaContacto() {
        return fechaContacto;
    }

    public void setFechaContacto(LocalDateTime fechaContacto) {
        this.fechaContacto = fechaContacto;
    }

    public String getComentario() {
        return comentario;
    }

    public void setComentario(String comentario) {
        this.comentario = comentario;
    }
}
