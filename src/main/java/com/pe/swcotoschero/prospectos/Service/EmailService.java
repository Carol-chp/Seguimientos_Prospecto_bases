package com.pe.swcotoschero.prospectos.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pe.swcotoschero.prospectos.Entity.ConfiguracionDueno;
import com.pe.swcotoschero.prospectos.Entity.Usuario;
import com.pe.swcotoschero.prospectos.Repository.ConfiguracionDuenoRepository;
import com.pe.swcotoschero.prospectos.Repository.UsuarioRepository;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.Map;

/**
 * RF-07 — Resumen diario por email al dueño (9pm).
 *
 * Parametrizado por entorno: si app.mail.enabled=false (o sin credenciales) el
 * bean JavaMailSender no existe → el envío se "salta" registrando el motivo
 * (el sistema sigue funcionando con normalidad). Reutiliza el catálogo de
 * métricas de ReportesService (RF-11: misma "verdad" que el dashboard).
 *
 * RF-06a/RF-06b (instantáneo / digest) → Fase 2.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static final long ADMIN_ROL_ID = 1L;
    private static final int MAX_INTENTOS = 3;

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final ReportesService reportesService;
    private final ConfiguracionDuenoRepository configRepo;
    private final UsuarioRepository usuarioRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${spring.mail.username:}")
    private String fromAddress;

    /**
     * Gate explícito. Spring Boot auto-configura un JavaMailSender desde
     * spring.mail.host aunque no haya credenciales; sin este flag el envío
     * intentaría (y fallaría 3 veces) contra Gmail sin auth cada noche.
     */
    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    public EmailService(ObjectProvider<JavaMailSender> mailSenderProvider,
                        ReportesService reportesService,
                        ConfiguracionDuenoRepository configRepo,
                        UsuarioRepository usuarioRepository) {
        this.mailSenderProvider = mailSenderProvider;
        this.reportesService = reportesService;
        this.configRepo = configRepo;
        this.usuarioRepository = usuarioRepository;
    }

    /** Resultado de un intento de envío (para el endpoint manual y el log). */
    public record ResultadoEnvio(boolean enviado, String motivo) {}

    /** Disparador asíncrono (lo usa el scheduler 9pm). Nunca bloquea. */
    @Async
    public void enviarResumenDiarioAsync() {
        try {
            enviarResumenDiario();
        } catch (Exception e) {
            log.error("Resumen diario: error no controlado", e);
        }
    }

    /**
     * Construye y envía el resumen diario. Síncrono (el endpoint manual usa el
     * resultado). Registra el estado del envío en ConfiguracionDueno.
     */
    @Transactional
    public ResultadoEnvio enviarResumenDiario() {
        ConfiguracionDueno cfg = configRepo.findTopByOrderByIdAsc()
                .orElseGet(ConfiguracionDueno::new);

        if (!Boolean.TRUE.equals(cfg.getToggleResumenDiario())) {
            return registrarEstado(cfg, false, "Resumen diario desactivado (toggle off).");
        }

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (!mailEnabled || mailSender == null) {
            return registrarEstado(cfg, false,
                    "Email no configurado (app.mail.enabled=false o sin credenciales). "
                    + "Configure MAIL_ENABLED=true + MAIL_USERNAME + MAIL_APP_PASSWORD.");
        }

        Usuario dueno = usuarioRepository
                .findByRol_IdAndEstadoOrderByNombreAsc(ADMIN_ROL_ID, true)
                .stream().findFirst().orElse(null);
        if (dueno == null || dueno.getEmail() == null || dueno.getEmail().isBlank()) {
            return registrarEstado(cfg, false, "El dueño no tiene email configurado.");
        }

        final String html;
        final byte[] excel;
        try {
            JsonNode dash = objectMapper.valueToTree(reportesService.calcularDashboard());
            html = construirHtml(dash);
            excel = reportesService.exportarProspectos(null, null, null);
        } catch (Exception e) {
            log.error("Resumen diario: error generando contenido", e);
            return registrarEstado(cfg, false, "Error generando el contenido: " + e.getMessage());
        }

        String asunto = "[Prospectos] Resumen del día " + LocalDate.now();
        Exception ultimoError = null;
        for (int intento = 1; intento <= MAX_INTENTOS; intento++) {
            try {
                MimeMessage msg = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
                if (fromAddress != null && !fromAddress.isBlank()) {
                    helper.setFrom(fromAddress);
                }
                helper.setTo(dueno.getEmail());
                helper.setSubject(asunto);
                helper.setText(html, true);
                helper.addAttachment("resumen_prospectos.xlsx",
                        new ByteArrayResource(excel),
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
                mailSender.send(msg);
                log.info("Resumen diario enviado a {} (intento {})", dueno.getEmail(), intento);
                return registrarEstado(cfg, true,
                        "Enviado a " + dueno.getEmail() + " (intento " + intento + ").");
            } catch (Exception e) {
                ultimoError = e;
                log.warn("Resumen diario: fallo intento {}/{}: {}",
                        intento, MAX_INTENTOS, e.getMessage());
            }
        }
        return registrarEstado(cfg, false,
                "Falló el envío tras " + MAX_INTENTOS + " intentos: "
                + (ultimoError != null ? ultimoError.getMessage() : "desconocido"));
    }

    private ResultadoEnvio registrarEstado(ConfiguracionDueno cfg, boolean ok, String detalle) {
        try {
            cfg.setUltimoEnvioResumenOk(ok);
            cfg.setUltimoEnvioResumenFecha(LocalDateTime.now());
            cfg.setUltimoEnvioResumenDetalle(detalle.length() > 500
                    ? detalle.substring(0, 500) : detalle);
            if (cfg.getId() != null) {
                configRepo.save(cfg);
            }
        } catch (Exception e) {
            log.warn("No se pudo registrar el estado de envío: {}", e.getMessage());
        }
        if (!ok) {
            log.info("Resumen diario NO enviado: {}", detalle);
        }
        return new ResultadoEnvio(ok, detalle);
    }

    /** Estado del último envío (para "aviso en el dashboard"). */
    public Map<String, Object> estadoUltimoEnvio() {
        ConfiguracionDueno cfg = configRepo.findTopByOrderByIdAsc()
                .orElseGet(ConfiguracionDueno::new);
        return Map.of(
                "ok", cfg.getUltimoEnvioResumenOk() != null ? cfg.getUltimoEnvioResumenOk() : false,
                "fecha", cfg.getUltimoEnvioResumenFecha() != null
                        ? cfg.getUltimoEnvioResumenFecha().toString() : "",
                "detalle", cfg.getUltimoEnvioResumenDetalle() != null
                        ? cfg.getUltimoEnvioResumenDetalle() : "Sin envíos registrados.",
                "toggleResumenDiario", Boolean.TRUE.equals(cfg.getToggleResumenDiario()),
                "mailConfigurado", mailEnabled && mailSenderProvider.getIfAvailable() != null);
    }

    // ---------------------------------------------------------------------
    // HTML del resumen — genérico sobre el JSON del dashboard (RF-11)
    // ---------------------------------------------------------------------

    private String construirHtml(JsonNode d) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div style=\"font-family:Arial,sans-serif;color:#222\">");
        sb.append("<h2 style=\"color:#5b3df5\">Resumen del día — ")
          .append(LocalDate.now()).append("</h2>");

        sb.append(tarjetas("Hoy", d.path("dia")));
        sb.append(tarjetas("Este mes", d.path("mes")));

        sb.append("<h3>Ranking por colaborador</h3>");
        sb.append("<table border=\"1\" cellpadding=\"6\" cellspacing=\"0\" "
                + "style=\"border-collapse:collapse;font-size:13px\">");
        sb.append("<tr style=\"background:#f0f0fb\"><th>Colaborador</th><th>Ventas</th>"
                + "<th>Derivados</th><th>Atenciones</th><th>Contactabilidad</th>"
                + "<th>Última actividad</th></tr>");
        for (JsonNode r : d.path("ranking")) {
            sb.append("<tr><td>").append(txt(r, "nombre"))
              .append("</td><td align=\"center\">").append(txt(r, "ventasCerradas"))
              .append("</td><td align=\"center\">").append(txt(r, "derivados"))
              .append("</td><td align=\"center\">").append(txt(r, "atenciones"))
              .append("</td><td align=\"center\">").append(pct(r, "contactabilidad"))
              .append("</td><td>").append(txt(r, "ultimaActividad"))
              .append("</td></tr>");
        }
        sb.append("</table>");

        JsonNode e = d.path("embudo");
        sb.append("<h3>Embudo</h3><p>")
          .append("Asignados ").append(txt(e, "asignados"))
          .append(" → Gestionados ").append(txt(e, "gestionados"))
          .append(" → Contactados(titular) ").append(txt(e, "contactadosTitular"))
          .append(" → Interesados ").append(txt(e, "interesados"))
          .append(" → Derivados ").append(txt(e, "derivados"))
          .append(" → <b>Ventas ").append(txt(e, "ventas")).append("</b></p>");

        sb.append("<p><b>Por cerrar (derivados pendientes):</b> ")
          .append(d.path("porCerrar").asText("0")).append("</p>");

        sb.append("<h3>Estado de bases</h3>");
        sb.append("<table border=\"1\" cellpadding=\"6\" cellspacing=\"0\" "
                + "style=\"border-collapse:collapse;font-size:13px\">");
        sb.append("<tr style=\"background:#f0f0fb\"><th>Base</th><th>Cantidad</th>"
                + "<th>Asignados</th><th>Sin asignar</th><th>Avance</th></tr>");
        for (JsonNode b : d.path("bases")) {
            sb.append("<tr><td>").append(txt(b, "nombre"))
              .append("</td><td align=\"center\">").append(txt(b, "cantidad"))
              .append("</td><td align=\"center\">").append(txt(b, "asignados"))
              .append("</td><td align=\"center\">").append(txt(b, "sinAsignar"))
              .append("</td><td align=\"center\">").append(pct(b, "avancePct"))
              .append("</td></tr>");
        }
        sb.append("</table>");

        sb.append("<p style=\"color:#888;font-size:12px;margin-top:24px\">"
                + "Generado automáticamente por Prospectos. Adjunto: Excel con el detalle.</p>");
        sb.append("</div>");
        return sb.toString();
    }

    private String tarjetas(String titulo, JsonNode n) {
        StringBuilder sb = new StringBuilder("<h3>").append(titulo).append("</h3><p>");
        Iterator<Map.Entry<String, JsonNode>> it = n.fields();
        while (it.hasNext()) {
            Map.Entry<String, JsonNode> f = it.next();
            sb.append("<span style=\"display:inline-block;margin:4px 14px 4px 0\">")
              .append("<b>").append(f.getValue().asText("0")).append("</b> ")
              .append(humanizar(f.getKey())).append("</span>");
        }
        return sb.append("</p>").toString();
    }

    private static String txt(JsonNode n, String f) {
        JsonNode v = n.path(f);
        return v.isMissingNode() || v.isNull() ? "—" : v.asText();
    }

    private static String pct(JsonNode n, String f) {
        JsonNode v = n.path(f);
        if (v.isMissingNode() || v.isNull()) return "—";
        return Math.round(v.asDouble() * 100) + "%";
    }

    private static String humanizar(String k) {
        return k.replaceAll("([a-z])([A-Z])", "$1 $2").toLowerCase();
    }
}
