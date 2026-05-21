package com.pe.swcotoschero.prospectos.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * Actualización de la configuración del dueño (toggles, metas, parámetros 6b.1).
 * Campos null = no se modifican (patch parcial).
 */
@Getter
@Setter
public class ConfiguracionRequest {
    private Boolean toggleEmailInstantaneo;
    private Boolean toggleEmailDigest;
    private Boolean toggleResumenDiario;
    private Integer metaVentasMensual;
    private Integer metaDerivadosPorColaborador;
    private Integer plazoReevaluacionSbsDias;
    private Integer maxIntentosNoContesto;
    private Integer plazoSeguimientoInteresadoDias;
    private String reglaReintentoNoContesto;
    private String plantillaWhatsapp;
    private String horaInicioJornada;
    private Integer minutosGraciaAusencia;
    /**
     * Email destinatario de reportes (resumen diario + notificaciones de atención).
     * Null = no modificar. Blank = limpiar (volverá al fallback del admin).
     * Formato email básico validado en el controlador si no es null/blank.
     */
    private String emailReportes;
}
