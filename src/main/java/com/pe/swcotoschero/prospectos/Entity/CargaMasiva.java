package com.pe.swcotoschero.prospectos.Entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "cargamasiva")
public class CargaMasiva {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombrearchivo")
    private String nombrearchivo;

    @Column(name = "fecha")
    private LocalDateTime fecha;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_asignado_id")
    private Usuario usuarioAsignado;

    @Column(name = "cantidad_prospectos")
    private Integer cantidadProspectos = 0;

    @Column(name = "estado_asignacion")
    private String estadoAsignacion = "SIN_ASIGNAR"; // SIN_ASIGNAR, ASIGNADO

    @Column(name = "fecha_asignacion")
    private LocalDateTime fechaAsignacion;
}
