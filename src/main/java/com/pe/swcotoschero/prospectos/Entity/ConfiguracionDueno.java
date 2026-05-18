package com.pe.swcotoschero.prospectos.Entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Configuracion global del dueno del sistema (singleton — un solo registro).
 * Almacena toggles de notificaciones, metas y parametros operativos.
 * Todos los campos tienen valores por defecto seguros (todo off, metas 0).
 */
@Entity
@Getter
@Setter
@Table(name = "configuracion_dueno")
public class ConfiguracionDueno {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // -------------------------------------------------------------------------
    // Toggles de email (RF-06a / RF-06b / RF-07 / RF-08)
    // -------------------------------------------------------------------------

    /** RF-06a: enviar email instantaneo al dueno por cada atencion registrada. */
    @Column(name = "toggle_email_instantaneo", nullable = false)
    private Boolean toggleEmailInstantaneo = false;

    /** RF-06b: enviar digest agrupado cada 5 atenciones de un colaborador. */
    @Column(name = "toggle_email_digest", nullable = false)
    private Boolean toggleEmailDigest = false;

    /** RF-07: enviar resumen diario a las 9pm al dueno. */
    @Column(name = "toggle_resumen_diario", nullable = false)
    private Boolean toggleResumenDiario = false;

    // -------------------------------------------------------------------------
    // Metas configurables (5f)
    // -------------------------------------------------------------------------

    /** Meta mensual de ventas cerradas (GANADO). 0 = sin meta definida. */
    @Column(name = "meta_ventas_mensual")
    private Integer metaVentasMensual = 0;

    /** Meta mensual de derivados por colaborador. 0 = sin meta definida. */
    @Column(name = "meta_derivados_por_colaborador")
    private Integer metaDerivadosPorColaborador = 0;

    // -------------------------------------------------------------------------
    // Parametros operativos (6b.1)
    // -------------------------------------------------------------------------

    /** Plazo por defecto (dias) para re-evaluacion SBS tras OBSERVADO. Default: 90 dias (3 meses). */
    @Column(name = "plazo_reevaluacion_sbs_dias", nullable = false)
    private Integer plazoReevaluacionSbsDias = 90;

    /** Maximo de intentos NO_CONTESTO antes de marcar ILOCALIZABLE. Default: 6. */
    @Column(name = "max_intentos_no_contesto", nullable = false)
    private Integer maxIntentosNoContesto = 6;

    /**
     * Regla de reintento escalonada serializada como JSON o CSV de horas.
     * Default (6b.1): [3h, 24h, 48h, 72h, 120h] -> "+3h,+24h,+48h,+72h,+120h"
     * El servicio que calcula proximaLlamada lee este campo.
     */
    @Column(name = "regla_reintento_no_contesto", length = 200)
    private String reglaReintentoNoContesto = "+3h,+24h,+48h,+72h,+120h";

    /** Hora de inicio de jornada esperada (HH:mm). Default: 09:00. */
    @Column(name = "hora_inicio_jornada", length = 5)
    private String horaInicioJornada = "09:00";

    /** Minutos de gracia tras la hora de inicio antes de marcar ausencia. Default: 45. */
    @Column(name = "minutos_gracia_ausencia", nullable = false)
    private Integer minutosGraciaAusencia = 45;

    // -------------------------------------------------------------------------
    // Estado del último envío del resumen diario (RF-07 — registro + aviso)
    // -------------------------------------------------------------------------

    @Column(name = "ultimo_envio_resumen_ok")
    private Boolean ultimoEnvioResumenOk;

    @Column(name = "ultimo_envio_resumen_fecha")
    private java.time.LocalDateTime ultimoEnvioResumenFecha;

    @Column(name = "ultimo_envio_resumen_detalle", length = 500)
    private String ultimoEnvioResumenDetalle;
}
