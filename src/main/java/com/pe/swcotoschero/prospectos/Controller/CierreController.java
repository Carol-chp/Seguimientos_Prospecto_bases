package com.pe.swcotoschero.prospectos.Controller;

import com.pe.swcotoschero.prospectos.Entity.Usuario;
import com.pe.swcotoschero.prospectos.Repository.UsuarioRepository;
import com.pe.swcotoschero.prospectos.Service.CierreService;
import com.pe.swcotoschero.prospectos.dto.CierreVentaRequest;
import com.pe.swcotoschero.prospectos.dto.NoCerroRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Cierre de venta por el DUEÑO (5c.bis / D4). Solo ADMINISTRADOR.
 */
@RestController
@RequestMapping("/api/cierre")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class CierreController {

    private final CierreService cierreService;
    private final UsuarioRepository usuarioRepository;

    public CierreController(CierreService cierreService, UsuarioRepository usuarioRepository) {
        this.cierreService = cierreService;
        this.usuarioRepository = usuarioRepository;
    }

    private Usuario adminAutenticado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return usuarioRepository.findByUsuarioAndEstado(auth.getName(), true)
                .orElseThrow(() -> new IllegalArgumentException("Usuario autenticado no encontrado"));
    }

    /** Lista de casos DERIVADO esperando cierre ("Por cerrar"). */
    @GetMapping("/por-cerrar")
    public ResponseEntity<Map<String, Object>> porCerrar(
            @RequestParam(defaultValue = "1") int pagina,
            @RequestParam(defaultValue = "10") int tamanioPagina) {
        return ResponseEntity.ok(cierreService.listarPorCerrar(pagina, tamanioPagina));
    }

    /** Registra la VENTA → GANADO. fechaElegibilidad obligatoria. */
    @PostMapping("/{asignacionId}/venta")
    public ResponseEntity<Map<String, Object>> registrarVenta(
            @PathVariable Long asignacionId,
            @RequestBody CierreVentaRequest req) {
        return ResponseEntity.ok(cierreService.registrarVenta(
                asignacionId, req.getFechaElegibilidad(), req.getComentario(), adminAutenticado()));
    }

    /** El dueño no cerró: REINTENTAR (con fecha) o DESCARTAR. */
    @PostMapping("/{asignacionId}/no-cerro")
    public ResponseEntity<Map<String, Object>> noCerro(
            @PathVariable Long asignacionId,
            @RequestBody NoCerroRequest req) {
        return ResponseEntity.ok(cierreService.noCerro(
                asignacionId, req.getAccion(), req.getFecha(), req.getComentario(),
                adminAutenticado()));
    }
}
