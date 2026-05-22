package com.pe.swcotoschero.prospectos.Controller;

import com.pe.swcotoschero.prospectos.Entity.Usuario;
import com.pe.swcotoschero.prospectos.Repository.UsuarioRepository;
import com.pe.swcotoschero.prospectos.Service.ContactoService;
import com.pe.swcotoschero.prospectos.dto.AperturaResponseDTO;
import com.pe.swcotoschero.prospectos.dto.ContactoRegistroDTO;
import com.pe.swcotoschero.prospectos.dto.EnviarBancoRequestDTO;
import com.pe.swcotoschero.prospectos.dto.HistorialContactoDTO;
import com.pe.swcotoschero.prospectos.dto.VerificacionSbsRequestDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Endpoints del Wizard de atencion (RF-04/13/14/15/16).
 *
 * Todos los endpoints requieren autenticacion JWT (configurado en SecurityConfig).
 * El GlobalExceptionHandler convierte IllegalArgumentException → 400 con mensaje.
 *
 * Contrato de endpoints:
 *
 * POST /api/contactos/apertura
 *   Body: { "prospectoId": N }
 *   200: { "aperturaId": N, "inicio": "ISO" }
 *
 * POST /api/contactos/apertura/{id}/cerrar
 *   Body: ninguno
 *   200: { "ok": true }
 *
 * POST /api/contactos/verificacion-sbs
 *   Body: { "prospectoId":N, "resultado":"APTO|OBSERVADO",
 *           "fechaReevaluacion":"YYYY-MM-DD"(opc), "comentario":"..."(opc) }
 *   200 APTO:     { "continuar": true }
 *   200 OBSERVADO: { "continuar": false, "estado": "EN_SEGUIMIENTO", "fechaReevaluacionSbs": "YYYY-MM-DD" }
 *
 * POST /api/contactos
 *   Body: ContactoRegistroDTO (ver DTO)
 *   200: { "ok": true, "estado": "...", "proximaLlamada": "ISO|null" }
 *
 * GET /api/contactos/historial/{prospectoId}
 *   200: [ { "fechaContacto", "resultado", "submotivoNoContesto", "quienContesto",
 *             "verificacionSbs", "comentario", "duracionGestion" } ]
 */
@RestController
@RequestMapping("/api/contactos")
public class ContactoController {

    private final ContactoService contactoService;
    private final UsuarioRepository usuarioRepository;

    public ContactoController(ContactoService contactoService,
                               UsuarioRepository usuarioRepository) {
        this.contactoService = contactoService;
        this.usuarioRepository = usuarioRepository;
    }

    // -------------------------------------------------------------------------
    // RF-14 — Apertura del modal
    // -------------------------------------------------------------------------

    @PostMapping("/apertura")
    public ResponseEntity<AperturaResponseDTO> abrirModal(
            @RequestBody Map<String, Long> body) {
        Long prospectoId = body.get("prospectoId");
        if (prospectoId == null) {
            throw new IllegalArgumentException("prospectoId es obligatorio.");
        }
        Usuario caller = obtenerUsuarioAutenticado();
        AperturaResponseDTO respuesta = contactoService.abrirModal(prospectoId, caller.getId());
        return ResponseEntity.ok(respuesta);
    }

    @PostMapping("/apertura/{id}/cerrar")
    public ResponseEntity<Map<String, Object>> cerrarModal(@PathVariable Long id) {
        Usuario caller = obtenerUsuarioAutenticado();
        contactoService.cerrarModalSinRegistro(id, caller.getId());
        return ResponseEntity.ok(Map.of("ok", true));
    }

    // -------------------------------------------------------------------------
    // RF-15 — Verificacion SBS
    // -------------------------------------------------------------------------

    @PostMapping("/verificacion-sbs")
    public ResponseEntity<Map<String, Object>> verificarSbs(
            @RequestBody VerificacionSbsRequestDTO dto) {
        Usuario caller = obtenerUsuarioAutenticado();
        Map<String, Object> resultado = contactoService.verificarSbs(dto, caller.getId());
        return ResponseEntity.ok(resultado);
    }

    // -------------------------------------------------------------------------
    // RF-04 / RF-13 / RF-16 — Registrar atencion
    // -------------------------------------------------------------------------

    @PostMapping
    public ResponseEntity<Map<String, Object>> registrarContacto(
            @RequestBody ContactoRegistroDTO dto) {
        Usuario usuarioAutenticado = obtenerUsuarioAutenticado();
        Map<String, Object> resultado = contactoService.registrarContacto(dto, usuarioAutenticado);
        return ResponseEntity.ok(resultado);
    }

    // -------------------------------------------------------------------------
    // BK-2 — Enviar al banco destino (caso OBSERVADO)
    // -------------------------------------------------------------------------

    /**
     * POST /api/contactos/enviar-banco
     *
     * El colaborador reenvía un prospecto OBSERVADO al banco destino.
     * El ciclo actual se cierra como DESCARTADO; el prospecto cambia de banco
     * y queda sin asignación activa para que el dueño lo reasigne.
     *
     * Body: { "prospectoId": N }
     * 200: { "ok": true, "bancoDestino": "nombre" }
     * 400: si el ciclo no es OBSERVADO o no hay banco destino configurado.
     */
    @PostMapping("/enviar-banco")
    public ResponseEntity<Map<String, Object>> enviarBanco(
            @RequestBody EnviarBancoRequestDTO dto) {
        if (dto.getProspectoId() == null) {
            throw new IllegalArgumentException("prospectoId es obligatorio.");
        }
        Usuario caller = obtenerUsuarioAutenticado();
        Map<String, Object> resultado =
                contactoService.enviarABancoDestino(dto.getProspectoId(), caller.getId());
        return ResponseEntity.ok(resultado);
    }

    // -------------------------------------------------------------------------
    // Historial del prospecto
    // -------------------------------------------------------------------------

    @GetMapping("/historial/{prospectoId}")
    public ResponseEntity<List<HistorialContactoDTO>> historial(
            @PathVariable Long prospectoId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Usuario caller = obtenerUsuarioAutenticado();
        boolean esAdmin = auth.getAuthorities()
                .contains(new SimpleGrantedAuthority("ROLE_ADMINISTRADOR"));
        List<HistorialContactoDTO> historial =
                contactoService.obtenerHistorial(prospectoId, caller.getId(), esAdmin);
        return ResponseEntity.ok(historial);
    }

    // -------------------------------------------------------------------------
    // Helper: usuario autenticado desde SecurityContext
    // -------------------------------------------------------------------------

    private Usuario obtenerUsuarioAutenticado() {
        String username = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();
        return usuarioRepository.findByUsuarioAndEstado(username, true)
                .orElseThrow(() -> new IllegalStateException(
                        "Usuario autenticado no encontrado en BD: " + username));
    }
}
