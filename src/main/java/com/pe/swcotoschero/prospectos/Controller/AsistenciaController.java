package com.pe.swcotoschero.prospectos.Controller;

import com.pe.swcotoschero.prospectos.Service.AsistenciaService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** RF-22 — asistencia del día (para el dueño; lo embebe el dashboard/email en 2.4). */
@RestController
@RequestMapping("/api/asistencia")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class AsistenciaController {

    private final AsistenciaService asistenciaService;

    public AsistenciaController(AsistenciaService asistenciaService) {
        this.asistenciaService = asistenciaService;
    }

    @GetMapping("/hoy")
    public ResponseEntity<Map<String, Object>> hoy() {
        return ResponseEntity.ok(asistenciaService.asistenciaHoy());
    }
}
