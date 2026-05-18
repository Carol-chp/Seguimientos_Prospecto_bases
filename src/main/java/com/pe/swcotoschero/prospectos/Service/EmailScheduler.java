package com.pe.swcotoschero.prospectos.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * RF-07 — dispara el resumen diario a la hora configurada
 * (cron `app.mail.resumen-cron`, zona `app.mail.zona`; default 21:00 America/Lima).
 * El propio EmailService decide si envía o se salta (toggle / sin credenciales).
 */
@Component
public class EmailScheduler {

    private static final Logger log = LoggerFactory.getLogger(EmailScheduler.class);
    private final EmailService emailService;

    public EmailScheduler(EmailService emailService) {
        this.emailService = emailService;
    }

    @Scheduled(cron = "${app.mail.resumen-cron:0 0 21 * * *}", zone = "${app.mail.zona:America/Lima}")
    public void resumenDiario() {
        log.info("Scheduler: disparando resumen diario (RF-07)");
        emailService.enviarResumenDiarioAsync();
    }
}
