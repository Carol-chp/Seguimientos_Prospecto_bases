package com.pe.swcotoschero.prospectos.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
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
import java.time.LocalTime;
import java.util.Iterator;
import java.util.List;
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
    private final com.pe.swcotoschero.prospectos.Repository.ContactoRepository contactoRepository;
    // findAndRegisterModules(): registra JavaTimeModule (jackson-datatype-jsr310,
    // ya en el classpath) para serializar LocalDateTime del DashboardDTO.
    // Fechas como ISO-8601 legible (no array de números) para el correo.
    private final ObjectMapper objectMapper = new ObjectMapper()
            .findAndRegisterModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

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
                        UsuarioRepository usuarioRepository,
                        com.pe.swcotoschero.prospectos.Repository.ContactoRepository contactoRepository) {
        this.mailSenderProvider = mailSenderProvider;
        this.reportesService = reportesService;
        this.configRepo = configRepo;
        this.usuarioRepository = usuarioRepository;
        this.contactoRepository = contactoRepository;
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

        // Resolver destinatario: emailReportes tiene prioridad; fallback al correo del admin.
        String destinatario = resolverDestinatario(cfg);
        if (destinatario == null) {
            return registrarEstado(cfg, false,
                    "Sin destinatario configurado: configure emailReportes o el correo del administrador.");
        }

        // Deduplicación entre réplicas (RF-07): el backend corre con varias
        // réplicas y cada una dispara el scheduler. Reclamo atómico del día →
        // solo UNA instancia continúa y envía; el resto se omite (un correo/día).
        if (cfg.getId() != null) {
            int claim = configRepo.reclamarEnvioDelDia(
                    cfg.getId(), LocalDateTime.now(), LocalDate.now().atStartOfDay());
            if (claim == 0) {
                log.info("Resumen diario: ya gestionado hoy por otra instancia; se omite.");
                return new ResultadoEnvio(false,
                        "Resumen del día ya gestionado por otra instancia.");
            }
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
                helper.setTo(destinatario);
                helper.setSubject(asunto);
                helper.setText(html, true);
                helper.addAttachment("resumen_prospectos.xlsx",
                        new ByteArrayResource(excel),
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
                mailSender.send(msg);
                log.info("Resumen diario enviado a {} (intento {})", destinatario, intento);
                return registrarEstado(cfg, true,
                        "Enviado a " + destinatario + " (intento " + intento + ").");
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
    // RF-06a / RF-06b — notificación por atención (instantáneo / digest c/5)
    // ---------------------------------------------------------------------

    /**
     * Notifica al dueño tras registrar una atención (best-effort, async,
     * NUNCA bloquea el registro). Respeta los toggles y app.mail.enabled.
     * RF-06a: email instantáneo por atención (si toggle).
     * RF-06b: digest cuando el colaborador llega a múltiplos de 5 atenciones hoy.
     */
    @org.springframework.scheduling.annotation.Async
    @Transactional(readOnly = true)
    public void notificarAtencionAsync(Long contactoId) {
        try {
            if (!mailEnabled) return;
            var mailSender = mailSenderProvider.getIfAvailable();
            if (mailSender == null || contactoId == null) return;

            ConfiguracionDueno cfg = configRepo.findTopByOrderByIdAsc()
                    .orElseGet(ConfiguracionDueno::new);
            boolean inst = Boolean.TRUE.equals(cfg.getToggleEmailInstantaneo());
            boolean digest = Boolean.TRUE.equals(cfg.getToggleEmailDigest());
            if (!inst && !digest) return;

            var contacto = contactoRepository.findById(contactoId).orElse(null);
            if (contacto == null || contacto.getAsignacion() == null) return;
            var asig = contacto.getAsignacion();
            var colaborador = asig.getUsuario();
            var prospecto = asig.getProspecto();
            if (colaborador == null || prospecto == null) return;

            // Resolver destinatario con la misma lógica que enviarResumenDiario.
            String destinatarioNotif = resolverDestinatario(cfg);
            if (destinatarioNotif == null) {
                log.info("notificarAtencion: sin destinatario configurado; se omite.");
                return;
            }

            String colab = (colaborador.getNombre() + " " + colaborador.getApellidos()).trim();
            String prosp = (prospecto.getNombre() + " " + prospecto.getApellido()).trim()
                    + " — cel " + mask(prospecto.getCelular())
                    + " / DNI " + mask(prospecto.getDocumentoIdentidad());
            String resultado = contacto.getEstadoResultado() != null
                    ? contacto.getEstadoResultado().name()
                    : (contacto.getVerificacionSbs() != null
                        ? "SBS " + contacto.getVerificacionSbs().name() : "—");

            LocalDate hoy = LocalDate.now();
            List<com.pe.swcotoschero.prospectos.Entity.Contacto> delDia =
                    contactoRepository.findActividadDelDia(colaborador.getId(),
                            hoy.atStartOfDay(), hoy.atTime(LocalTime.MAX));
            int total = delDia.size();

            if (inst) {
                String html = "<div style=\"font-family:Arial,sans-serif\">"
                        + "<p><b>" + esc(colab) + "</b> registró una atención.</p>"
                        + "<p>Prospecto: " + esc(prosp) + "<br>"
                        + "Resultado: <b>" + esc(resultado) + "</b>"
                        + (contacto.getQuienContesto() != null
                            ? " (contestó: " + contacto.getQuienContesto().name() + ")" : "")
                        + "<br>Comentario: " + esc(nz(contacto.getComentario())) + "</p>"
                        + "<p style=\"color:#666\">Acumulado de hoy de " + esc(colab)
                        + ": " + total + " atenciones.</p></div>";
                enviarSimple(mailSender, destinatarioNotif,
                        "[Prospectos] " + colab + ": " + resultado + " — "
                        + prospecto.getNombre(), html);
            }

            if (digest && total > 0 && total % 5 == 0) {
                StringBuilder sb = new StringBuilder("<div style=\"font-family:Arial,sans-serif\">");
                sb.append("<h3>").append(esc(colab))
                  .append(" — últimas 5 atenciones</h3>");
                sb.append("<table border=\"1\" cellpadding=\"6\" "
                        + "style=\"border-collapse:collapse;font-size:13px\">"
                        + "<tr style=\"background:#f0f0fb\"><th>Hora</th><th>Resultado</th>"
                        + "<th>Quién</th><th>Comentario</th></tr>");
                delDia.stream().limit(5).forEach(c -> sb.append("<tr><td>")
                        .append(c.getFechaContacto() != null
                            ? c.getFechaContacto().toLocalTime().withNano(0) : "")
                        .append("</td><td>").append(c.getEstadoResultado() != null
                            ? c.getEstadoResultado().name() : "—")
                        .append("</td><td>").append(c.getQuienContesto() != null
                            ? c.getQuienContesto().name() : "—")
                        .append("</td><td>").append(esc(nz(c.getComentario())))
                        .append("</td></tr>"));
                sb.append("</table><p style=\"color:#666\">Acumulado de hoy: ")
                  .append(total).append(" atenciones.</p></div>");
                enviarSimple(mailSender, destinatarioNotif,
                        "[Prospectos] " + colab + ": 5 atenciones", sb.toString());
            }
        } catch (Exception e) {
            // best-effort: nunca propaga al registro de la atención
            log.warn("notificarAtencion: fallo best-effort: {}", e.getMessage());
        }
    }

    /**
     * Resuelve el destinatario efectivo para los correos de reportes.
     * Prioridad:
     *  1. cfg.emailReportes (configurado explícitamente por el dueño).
     *  2. Correo del primer usuario ADMINISTRADOR activo (fallback de compatibilidad).
     * Retorna null si ninguno está disponible; el caller debe abortar sin error visible.
     */
    private String resolverDestinatario(ConfiguracionDueno cfg) {
        if (cfg.getEmailReportes() != null && !cfg.getEmailReportes().isBlank()) {
            return cfg.getEmailReportes().trim();
        }
        // Fallback al correo del admin para no romper instalaciones existentes.
        Usuario admin = usuarioRepository
                .findByRol_IdAndEstadoOrderByNombreAsc(ADMIN_ROL_ID, true)
                .stream().findFirst().orElse(null);
        if (admin != null && admin.getEmail() != null && !admin.getEmail().isBlank()) {
            log.debug("emailReportes no configurado; usando correo del administrador como fallback.");
            return admin.getEmail().trim();
        }
        log.warn("Sin email de reportes: configure emailReportes en /api/reportes/config.");
        return null;
    }

    private void enviarSimple(JavaMailSender mailSender, String to,
                              String asunto, String html) {
        try {
            jakarta.mail.internet.MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper h = new MimeMessageHelper(msg, false, "UTF-8");
            if (fromAddress != null && !fromAddress.isBlank()) h.setFrom(fromAddress);
            h.setTo(to);
            h.setSubject(asunto);
            h.setText(html, true);
            mailSender.send(msg);
            log.info("Email '{}' enviado a {}", asunto, to);
        } catch (Exception e) {
            log.warn("Email '{}' falló (best-effort): {}", asunto, e.getMessage());
        }
    }

    private static String nz(String s) { return s == null ? "" : s; }

    /** Enmascara dejando los últimos 3 caracteres visibles (privacidad en correo). */
    private static String mask(String v) {
        if (v == null || v.isBlank()) return "";
        String t = v.trim();
        if (t.length() <= 3) return "***";
        return "*".repeat(t.length() - 3) + t.substring(t.length() - 3);
    }

    private static String esc(String s) {
        return s == null ? "" : s.replace("&", "&amp;")
                .replace("<", "&lt;").replace(">", "&gt;");
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

        // porCerrar eliminado: el colaborador cierra directamente (GANADO inmediato).

        // 2.4 — Asistencia del día / casos "En riesgo" (RF-22 / 5j)
        JsonNode as = d.path("asistencia");
        sb.append("<h3>Asistencia de hoy</h3>");
        if (!as.path("esLaborable").asBoolean(true)) {
            sb.append("<p>Hoy no es día laborable.</p>");
        } else {
            sb.append("<p>")
              .append(txt(as, "totalColaboradores")).append(" colaboradores · ")
              .append("<b style=\"color:")
              .append(as.path("totalAusentes").asInt(0) > 0 ? "#c0392b" : "#27ae60")
              .append("\">").append(as.path("totalAusentes").asText("0"))
              .append(" ausente(s)</b></p>");
            sb.append("<table border=\"1\" cellpadding=\"6\" cellspacing=\"0\" "
                    + "style=\"border-collapse:collapse;font-size:13px\">");
            sb.append("<tr style=\"background:#f0f0fb\"><th>Colaborador</th>"
                    + "<th>Jornada</th><th>Inicio</th><th>Estado</th></tr>");
            for (JsonNode c : as.path("colaboradores")) {
                boolean ausente = c.path("ausente").asBoolean(false);
                sb.append("<tr><td>").append(txt(c, "nombre"))
                  .append("</td><td align=\"center\">")
                  .append(c.path("jornadaIniciada").asBoolean(false) ? "Sí" : "No")
                  .append("</td><td align=\"center\">").append(txt(c, "inicio"))
                  .append("</td><td align=\"center\" style=\"color:")
                  .append(ausente ? "#c0392b\"><b>Ausente</b>" : "#27ae60\">Presente")
                  .append("</td></tr>");
            }
            sb.append("</table>");
        }
        long enRiesgo = d.path("porEnRiesgo").asLong(0);
        sb.append("<p><b>Casos \"En riesgo\" (por reasignar):</b> "
                + "<span style=\"color:")
          .append(enRiesgo > 0 ? "#c0392b" : "#27ae60")
          .append("\"><b>").append(enRiesgo).append("</b></span></p>");

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
