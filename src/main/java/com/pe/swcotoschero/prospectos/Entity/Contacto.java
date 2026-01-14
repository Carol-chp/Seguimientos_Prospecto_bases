package com.pe.swcotoschero.prospectos.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
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
    
    @Column(name = "contesto_llamada")
    private Boolean contestoLlamada;
    
    @Column(name = "interesado")
    private Boolean interesado;

}
