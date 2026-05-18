package com.pe.swcotoschero.prospectos.Entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Calendario laboral: dias feriados y no laborables (RF-22).
 * Precargado con feriados nacionales de Peru, editable por el dueno.
 * La deteccion de ausencia solo aplica en dias laborables (no en feriados ni domingos).
 */
@Entity
@Getter
@Setter
@Table(name = "calendario_laboral",
       uniqueConstraints = @UniqueConstraint(
               name = "uk_calendario_fecha",
               columnNames = {"fecha"}))
public class CalendarioLaboral {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    /** true = dia feriado / no laborable. */
    @Column(name = "es_feriado", nullable = false)
    private Boolean esFeriado = true;

    @Column(name = "descripcion", length = 200)
    private String descripcion;
}
