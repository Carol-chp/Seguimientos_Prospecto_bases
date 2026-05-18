package com.pe.swcotoschero.prospectos.Entity;

import com.pe.swcotoschero.prospectos.Entity.enums.EstadoGestion;
import com.pe.swcotoschero.prospectos.Entity.enums.QuienContesto;
import com.pe.swcotoschero.prospectos.Entity.enums.ResultadoAtencion;
import com.pe.swcotoschero.prospectos.Entity.enums.VerificacionSbs;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Ciclo de gestion = una oportunidad de venta sobre un prospecto.
 * Un prospecto puede tener multiples ciclos: uno GANADO historico + uno activo.
 * NO se deben poner constraints de unicidad prospecto<->usuario porque la re-elegibilidad
 * post-venta (D7) crea un ciclo nuevo sobre el mismo prospecto con el mismo colaborador.
 */
@Entity
@Getter
@Setter
@Table(name = "asignacion")
public class Asignacion {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long asignacionID;

    /**
     * Control de concurrencia optimista (RF seguridad).
     * Cada UPDATE incrementa este campo; si dos hilos intentan modificar
     * la misma version simultaneamente, el segundo recibe 409 Conflict.
     */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    // -------------------------------------------------------------------------
    // Relaciones principales
    // -------------------------------------------------------------------------

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prospecto_id", nullable = false)
    private Prospecto prospecto;

    /** Colaborador (teleoperador) dueno del ciclo. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "personal_id", nullable = false)
    private Usuario usuario;

    /** Administrador que realizo la asignacion original. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "administrador_id", nullable = false)
    private Usuario administrador;

    // -------------------------------------------------------------------------
    // Estado del ciclo (unico enum canonico — sin literales de texto)
    // -------------------------------------------------------------------------

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoGestion estado = EstadoGestion.SIN_GESTIONAR;

    /** Resultado de la ultima atencion registrada. Null si aun no se ha gestionado. */
    @Enumerated(EnumType.STRING)
    @Column(name = "estado_resultado", length = 20)
    private ResultadoAtencion estadoResultado;

    // -------------------------------------------------------------------------
    // Fechas base
    // -------------------------------------------------------------------------

    @Column(name = "fecha_asignacion")
    private LocalDateTime fechaAsignacion = LocalDateTime.now();

    /**
     * Fecha/hora para el proximo recontacto (agenda, reintento NO_CONTESTO, re-SBS).
     * Es la fuente de verdad para calcular la cola accionable (D2).
     * El estado NO cambia por un job — la cola filtra por esta fecha.
     */
    @Column(name = "fecha_agenda")
    private LocalDateTime fechaAgenda;

    // -------------------------------------------------------------------------
    // Verificacion SBS (RF-15)
    // -------------------------------------------------------------------------

    @Enumerated(EnumType.STRING)
    @Column(name = "verificacion_sbs", length = 15)
    private VerificacionSbs verificacionSbs;

    @Column(name = "fecha_consulta_sbs")
    private LocalDateTime fechaConsultaSbs;

    @Column(name = "fecha_reevaluacion_sbs")
    private LocalDate fechaReevaluacionSbs;

    // -------------------------------------------------------------------------
    // Llamada / wizard (RF-04, RF-13, RF-16)
    // -------------------------------------------------------------------------

    @Enumerated(EnumType.STRING)
    @Column(name = "quien_contesto", length = 15)
    private QuienContesto quienContesto;

    @Column(name = "submotivo_no_contesto", length = 15)
    private String submotivoNoContesto;

    /** Contador de intentos fallidos (NO_CONTESTO). Al superar el maximo -> ILOCALIZABLE. */
    @Column(name = "intentos_fallidos")
    private Integer intentosFallidos = 0;

    /** Fecha/hora sugerida para el proximo intento (calculada automaticamente, editable). */
    @Column(name = "proxima_llamada")
    private LocalDateTime proximaLlamada;

    /**
     * Duracion total del modal abierto -> guardado (segundos). RF-13.
     * Incluye tiempo de verificacion SBS (es "tiempo de gestion", no "tiempo de llamada").
     */
    @Column(name = "duracion_gestion")
    private Integer duracionGestion;

    /**
     * Marca interna del instante en que se resolvio la verificacion SBS dentro del modal.
     * No cambia la UX. Permite calcular tiempo SBS vs tiempo de llamada a futuro (D1).
     */
    @Column(name = "marca_fin_sbs")
    private LocalDateTime marcaFinSbs;

    // -------------------------------------------------------------------------
    // Derivacion y cierre (5c.bis, D4)
    // -------------------------------------------------------------------------

    /** Usuario colaborador que derivo el caso al dueno (autor de la transicion a DERIVADO). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "derivado_por_id")
    private Usuario derivadoPor;

    @Column(name = "fecha_derivacion")
    private LocalDateTime fechaDerivacion;

    /** Usuario administrador que registro la venta (cierre). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cerrado_por_id")
    private Usuario cerradoPor;

    @Column(name = "fecha_cierre")
    private LocalDateTime fechaCierre;

    // -------------------------------------------------------------------------
    // Re-elegibilidad post-venta (D7)
    // -------------------------------------------------------------------------

    /**
     * Fecha en que este prospecto puede ser contactado de nuevo para un nuevo prestamo.
     * Obligatoria al registrar GANADO. Al vencer, el job crea un ciclo nuevo.
     */
    @Column(name = "fecha_elegibilidad")
    private LocalDate fechaElegibilidad;

    /**
     * ID de la asignacion GANADO que origino este ciclo (enlace de cadena D7).
     * Null si es el primer ciclo del prospecto.
     */
    @Column(name = "ciclo_anterior_id")
    private Long cicloAnteriorId;

    // -------------------------------------------------------------------------
    // Auditoria de asignacion / reasignacion (5g, 5j)
    // -------------------------------------------------------------------------

    /** Usuario administrador que realizo la asignacion (puede diferir de administrador si hubo reasignacion). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asignado_por_id")
    private Usuario asignadoPor;

    @Column(name = "fecha_asignacion_registro")
    private LocalDateTime fechaAsignacionRegistro;

    /** ID de la asignacion anterior (ciclo activo del que proviene por reasignacion). */
    @Column(name = "reasignado_de_id")
    private Long reasignadoDeId;

    /** ID de la asignacion nueva (ciclo al que se reasigno). */
    @Column(name = "reasignado_para_id")
    private Long reasignadoParaId;

    @Column(name = "fecha_reasignacion")
    private LocalDateTime fechaReasignacion;

    @Column(name = "motivo_reasignacion", length = 500)
    private String motivoReasignacion;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    public Asignacion() {
        this.fechaAsignacion = LocalDateTime.now();
        this.estado = EstadoGestion.SIN_GESTIONAR;
        this.intentosFallidos = 0;
    }
}
