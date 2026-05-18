package com.pe.swcotoschero.prospectos.Controller;

import com.pe.swcotoschero.prospectos.Entity.ConfiguracionDueno;
import com.pe.swcotoschero.prospectos.Repository.ConfiguracionDuenoRepository;
import com.pe.swcotoschero.prospectos.Service.EmailService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Operación del resumen por email (RF-07/08). Solo ADMINISTRADOR.
 */
@RestController
@RequestMapping("/api/reportes")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class EmailController {

    private final EmailService emailService;
    private final ConfiguracionDuenoRepository configRepo;

    public EmailController(EmailService emailService, ConfiguracionDuenoRepository configRepo) {
        this.emailService = emailService;
        this.configRepo = configRepo;
    }

    /** Envía el resumen ahora (manual / prueba). Devuelve si se envió y el motivo. */
    @PostMapping("/enviar-resumen")
    public ResponseEntity<Map<String, Object>> enviarAhora() {
        EmailService.ResultadoEnvio r = emailService.enviarResumenDiario();
        return ResponseEntity.ok(Map.of("enviado", r.enviado(), "motivo", r.motivo()));
    }

    /** Enciende/apaga el toggle del resumen diario (RF-08). */
    @PutMapping("/config/resumen-diario")
    public ResponseEntity<Map<String, Object>> toggleResumen(@RequestParam boolean activo) {
        ConfiguracionDueno cfg = configRepo.findTopByOrderByIdAsc()
                .orElseThrow(() -> new IllegalStateException(
                        "No existe configuración del dueño (seed faltante)."));
        cfg.setToggleResumenDiario(activo);
        configRepo.save(cfg);
        return ResponseEntity.ok(Map.of("toggleResumenDiario", activo));
    }

    /** Estado del último envío — para avisar en el dashboard si falló. */
    @GetMapping("/estado-email")
    public ResponseEntity<Map<String, Object>> estado() {
        return ResponseEntity.ok(emailService.estadoUltimoEnvio());
    }
}
