package com.pe.swcotoschero.prospectos.Entity;

import com.pe.swcotoschero.prospectos.Entity.enums.QuienContesto;
import com.pe.swcotoschero.prospectos.Entity.enums.ResultadoAtencion;
import com.pe.swcotoschero.prospectos.Entity.enums.SubmotivoNoContesto;
import com.pe.swcotoschero.prospectos.Entity.enums.VerificacionSbs;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Historial de atenciones de un ciclo (Asignacion).
 * Cada llamada / interaccion registrada crea un Contacto nuevo (inmutable una vez guardado).
 */
@Entity
@Getter
@Setter
@Table(name = "contacto")
public class Contacto {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long contactoID;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asignacion_id", nullable = false)
    private Asignacion asignacion;

    @Column(name = "fecha_contacto")
    private LocalDateTime fechaContacto = LocalDateTime.now();

    private String comentario;

    // -------------------------------------------------------------------------
    // Resultado canonico (wizard RF-04)
    // -------------------------------------------------------------------------

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_resultado", length = 20)
    private ResultadoAtencion estadoResultado;

    @Enumerated(EnumType.STRING)
    @Column(name = "submotivo_no_contesto", length = 15)
    private SubmotivoNoContesto submotivoNoContesto;

    @Enumerated(EnumType.STRING)
    @Column(name = "quien_contesto", length = 15)
    private QuienContesto quienContesto;

    // -------------------------------------------------------------------------
    // Verificacion SBS del Paso 0 (RF-15)
    // -------------------------------------------------------------------------

    @Enumerated(EnumType.STRING)
    @Column(name = "verificacion_sbs", length = 15)
    private VerificacionSbs verificacionSbs;

    @Column(name = "fecha_consulta_sbs")
    private LocalDateTime fechaConsultaSbs;

    // -------------------------------------------------------------------------
    // Cronometro (RF-13)
    // -------------------------------------------------------------------------

    /** Duracion del modal abierto -> guardado en segundos (incluye tiempo SBS). */
    @Column(name = "duracion_gestion")
    private Integer duracionGestion;

    /** Marca interna del momento en que se resolvio SBS (para split futuro de tiempos). */
    @Column(name = "marca_fin_sbs")
    private LocalDateTime marcaFinSbs;

    // -------------------------------------------------------------------------
    // Campos de compatibilidad hacia atras (deprecados — usar estadoResultado)
    // -------------------------------------------------------------------------

    @Column(name = "contesto_llamada")
    @Deprecated
    private Boolean contestoLlamada;

    @Column(name = "interesado")
    @Deprecated
    private Boolean interesado;
}
