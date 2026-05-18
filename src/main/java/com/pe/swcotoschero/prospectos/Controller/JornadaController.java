package com.pe.swcotoschero.prospectos.Controller;

import com.pe.swcotoschero.prospectos.Service.JornadaService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** RF-21 — jornada del colaborador autenticado. */
@RestController
@RequestMapping("/api/jornada")
public class JornadaController {

    private final JornadaService jornadaService;

    public JornadaController(JornadaService jornadaService) {
        this.jornadaService = jornadaService;
    }

    private String username() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return a.getName();
    }

    @PostMapping("/iniciar")
    public ResponseEntity<Map<String, Object>> iniciar() {
        return ResponseEntity.ok(jornadaService.iniciar(username()));
    }

    @PostMapping("/finalizar")
    public ResponseEntity<Map<String, Object>> finalizar() {
        return ResponseEntity.ok(jornadaService.finalizar(username()));
    }

    @GetMapping("/hoy")
    public ResponseEntity<Map<String, Object>> hoy() {
        return ResponseEntity.ok(jornadaService.estadoHoy(username()));
    }
}
