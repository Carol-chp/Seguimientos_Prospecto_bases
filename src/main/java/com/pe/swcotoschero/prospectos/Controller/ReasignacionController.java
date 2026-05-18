package com.pe.swcotoschero.prospectos.Controller;

import com.pe.swcotoschero.prospectos.Entity.Usuario;
import com.pe.swcotoschero.prospectos.Repository.UsuarioRepository;
import com.pe.swcotoschero.prospectos.Service.ReasignacionService;
import com.pe.swcotoschero.prospectos.dto.ReasignacionRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** RF-23 — reasignación y bucket "En riesgo". Solo ADMINISTRADOR. */
@RestController
@RequestMapping("/api/reasignacion")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class ReasignacionController {

    private final ReasignacionService reasignacionService;
    private final UsuarioRepository usuarioRepository;

    public ReasignacionController(ReasignacionService reasignacionService,
                                  UsuarioRepository usuarioRepository) {
        this.reasignacionService = reasignacionService;
        this.usuarioRepository = usuarioRepository;
    }

    private Usuario admin() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return usuarioRepository.findByUsuarioAndEstado(a.getName(), true)
                .orElseThrow(() -> new IllegalArgumentException("Usuario autenticado no encontrado"));
    }

    /** Casos en riesgo (colaboradores ausentes hoy con vencidos/sin gestionar). */
    @GetMapping("/en-riesgo")
    public ResponseEntity<Map<String, Object>> enRiesgo() {
        return ResponseEntity.ok(reasignacionService.enRiesgo());
    }

    /** Reasigna una selección de casos al colaborador destino. */
    @PostMapping
    public ResponseEntity<Map<String, Object>> reasignar(@RequestBody ReasignacionRequest req) {
        return ResponseEntity.ok(reasignacionService.reasignar(
                req.getAsignacionIds(), req.getNuevoUsuarioId(), req.getMotivo(), admin()));
    }

    /** Reasigna TODOS los casos activos de un colaborador al destino. */
    @PostMapping("/colaborador/{origenId}")
    public ResponseEntity<Map<String, Object>> reasignarTodo(
            @PathVariable Long origenId, @RequestBody ReasignacionRequest req) {
        return ResponseEntity.ok(reasignacionService.reasignarTodoDeColaborador(
                origenId, req.getNuevoUsuarioId(), req.getMotivo(), admin()));
    }
}
