package com.pe.swcotoschero.prospectos.Entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Registro de jornada laboral de un colaborador (RF-21).
 * El colaborador marca entrada al empezar y salida al terminar.
 * Permite medir asistencia, puntualidad y horas activas.
 */
@Entity
@Getter
@Setter
@Table(name = "jornada",
       uniqueConstraints = @UniqueConstraint(
               name = "uk_jornada_usuario_fecha",
               columnNames = {"usuario_id", "fecha"}))
public class Jornada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    /** Fecha del dia de jornada (solo la fecha, sin hora). */
    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    /** Momento en que el colaborador marco "Iniciar jornada". */
    @Column(name = "inicio")
    private LocalDateTime inicio;

    /** Momento en que el colaborador marco "Finalizar jornada". */
    @Column(name = "fin")
    private LocalDateTime fin;
}
