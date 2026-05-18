package com.pe.swcotoschero.prospectos.Controller;

import com.pe.swcotoschero.prospectos.Service.JobDiarioService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** Disparo manual del job diario (1.7 / D2-D7). Solo ADMINISTRADOR. */
@RestController
@RequestMapping("/api/reportes")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class JobController {

    private final JobDiarioService jobDiarioService;

    public JobController(JobDiarioService jobDiarioService) {
        this.jobDiarioService = jobDiarioService;
    }

    @PostMapping("/job-diario")
    public ResponseEntity<Map<String, Object>> ejecutar() {
        return ResponseEntity.ok(jobDiarioService.ejecutar());
    }
}
