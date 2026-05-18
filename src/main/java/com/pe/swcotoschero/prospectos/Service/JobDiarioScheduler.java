package com.pe.swcotoschero.prospectos.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Dispara el job diario (1.7) a la hora configurada
 * (cron `app.job.diario-cron`, zona `app.job.zona`; default 00:30 America/Lima).
 */
@Component
public class JobDiarioScheduler {

    private static final Logger log = LoggerFactory.getLogger(JobDiarioScheduler.class);
    private final JobDiarioService jobDiarioService;

    public JobDiarioScheduler(JobDiarioService jobDiarioService) {
        this.jobDiarioService = jobDiarioService;
    }

    @Scheduled(cron = "${app.job.diario-cron:0 30 0 * * *}", zone = "${app.job.zona:America/Lima}")
    public void ejecutar() {
        log.info("Scheduler: disparando job diario (D2/D7)");
        try {
            jobDiarioService.ejecutar();
        } catch (Exception e) {
            log.error("Job diario: error no controlado", e);
        }
    }
}
