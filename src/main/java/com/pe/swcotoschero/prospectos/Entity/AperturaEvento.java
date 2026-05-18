package com.pe.swcotoschero.prospectos.Entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Evento de apertura del modal de atencion (RF-14).
 * Se registra CADA apertura del modal, incluso si no se guarda resultado.
 * Permite detectar "Abierto sin gestion": el colaborador vio el numero/DNI pero no registro nada.
 *
 * huboRegistro = false -> apertura sin gestion (posible fuga de info o llamada no registrada).
 * huboRegistro = true  -> apertura que termino en accion registrada (no cuenta como "sin gestion").
 */
@Entity
@Getter
@Setter
@Table(name = "apertura_evento")
public class AperturaEvento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asignacion_id", nullable = false)
    private Asignacion asignacion;

    @Column(name = "inicio", nullable = false)
    private LocalDateTime inicio;

    /** Momento en que se cerro el modal (ya sea guardando o cancelando). */
    @Column(name = "fin")
    private LocalDateTime fin;

    /**
     * true si la apertura termino en un registro de atencion guardado.
     * false (o null) si el colaborador cerro el modal sin guardar -> "Abierto sin gestion".
     */
    @Column(name = "hubo_registro")
    private Boolean huboRegistro = false;
}
