package com.pe.swcotoschero.prospectos.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
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

    @ManyToOne
    @JoinColumn(name = "campania_id")
    private Campania campania;

    @ManyToOne
    @JoinColumn(name = "idcargamasiva")
    private CargaMasiva cargaMasiva;

    @Column(name = "subcampania") // Ajuste para evitar problemas con tildes
    private String subcampania;
    
    @Column(name = "estado_interesado")
    private Boolean estadoInteresado;

    @Transient
    private String comentario;

    @Transient
    private String personal;

    @Override
    public String toString() {
        return "Prospecto{" +
                "nombre='" + nombre + '\'' +
                ", apellidos='" + apellido + '\'' +
                ", celular='" + celular + '\'' +
                ", documentoIdentidad='" + documentoIdentidad + '\'' +
                ", sexo='" + sexo + '\'' +
                ", banco='" + banco + '\'' +
                ", cargo='" + cargo + '\'' +
                ", distrito='" + distrito + '\'' +
                ", campania='" + campania + '\'' +
                ", subcampania='" + subcampania + '\'' +
                '}';
    }
}
