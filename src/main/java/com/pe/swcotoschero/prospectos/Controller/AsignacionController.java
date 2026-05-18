package com.pe.swcotoschero.prospectos.Controller;

import com.pe.swcotoschero.prospectos.Entity.Usuario;
import com.pe.swcotoschero.prospectos.Entity.enums.EstadoGestion;
import com.pe.swcotoschero.prospectos.Entity.enums.ResultadoAtencion;
import com.pe.swcotoschero.prospectos.Repository.AsignacionRepository;
import com.pe.swcotoschero.prospectos.Repository.UsuarioRepository;
import com.pe.swcotoschero.prospectos.Service.AsignacionService;
import com.pe.swcotoschero.prospectos.Service.ColaboradorColaService;
import com.pe.swcotoschero.prospectos.dto.AsignacionMultiRequest;
import com.pe.swcotoschero.prospectos.dto.MiActividadDTO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/asignaciones")
public class AsignacionController {

    private final AsignacionService asignacionService;
    private final ColaboradorColaService colaboradorColaService;
    private final UsuarioRepository usuarioRepository;
    private final AsignacionRepository asignacionRepository;

    public AsignacionController(AsignacionService asignacionService,
                                 ColaboradorColaService colaboradorColaService,
                                 UsuarioRepository usuarioRepository,
                                 AsignacionRepository asignacionRepository) {
        this.asignacionService = asignacionService;
        this.colaboradorColaService = colaboradorColaService;
        this.usuarioRepository = usuarioRepository;
        this.asignacionRepository = asignacionRepository;
    }

    // =========================================================================
    // Asignacion / administrador
    // =========================================================================

    /**
     * Asignar prospectos de una carga masiva a un usuario.
     */
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @PostMapping("/asignar-carga-masiva")
    public ResponseEntity<?> asignarCargaMasivaAUsuario(
            @RequestParam Long cargaMasivaId,
            @RequestParam Long usuarioId,
            @RequestParam(required = false) Integer cantidad) {

        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();

            Usuario usuarioAutenticado = usuarioRepository.findByUsuarioAndEstado(username, true)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Usuario autenticado no encontrado"));

            Map<String, Object> resultado = asignacionService.asignarCargaMasivaAUsuario(
                    cargaMasivaId, usuarioId, usuarioAutenticado.getId(), cantidad);
            return ResponseEntity.ok(resultado);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Error interno del servidor: " + e.getMessage()));
        }
    }

    /**
     * Reparte una carga a varios colaboradores en un solo flujo (RF-19).
     * Body JSON: { "cargaMasivaId": 1, "asignaciones": [ {"usuarioId":2,"cantidad":10}, ... ] }
     * Errores de validacion → 400 via GlobalExceptionHandler.
     */
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @PostMapping("/asignar-multi")
    public ResponseEntity<Map<String, Object>> asignarMulti(
            @RequestBody AsignacionMultiRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Usuario admin = usuarioRepository
                .findByUsuarioAndEstado(authentication.getName(), true)
                .orElseThrow(() -> new IllegalArgumentException("Usuario autenticado no encontrado"));

        return ResponseEntity.ok(asignacionService.asignarCargaMasivaMulti(
                request.getCargaMasivaId(), request.getAsignaciones(), admin.getId()));
    }

    // =========================================================================
    // Cola del colaborador (RF-17)
    // =========================================================================

    /**
     * Cola del colaborador autenticado con 11 filtros y busqueda de texto.
     *
     * Parametros:
     *   filtro        (default MI_COLA_HOY) — uno de los 11 valores de FiltroColaborador.
     *                 Valor invalido → 400 con lista de permitidos.
     *   busqueda      Texto libre (opcional). Match ILIKE sobre nombre, apellido,
     *                 celular y documentoIdentidad (valor real, respuesta enmascarada).
     *   pagina        (default 1) — 1-based.
     *   tamanioPagina (default 10).
     *
     * Compatibilidad: los parametros legacy estado/estadoResultado se ignoran
     * (el frontend nuevo debe usar filtro; el frontend antiguo que los mandaba
     * no rompe el arranque).
     */
    @GetMapping("/mis-prospectos")
    public ResponseEntity<?> getMisProspectos(
            @RequestParam(defaultValue = "MI_COLA_HOY") String filtro,
            @RequestParam(required = false) String busqueda,
            @RequestParam(defaultValue = "1") int pagina,
            @RequestParam(defaultValue = "10") int tamanioPagina,
            // Parametros legacy — aceptados para no romper el frontend antiguo; ignorados.
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String estadoResultado) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Usuario usuario = usuarioRepository
                .findByUsuarioAndEstado(authentication.getName(), true)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        // IllegalArgumentException (filtro invalido) → GlobalExceptionHandler → 400
        Map<String, Object> result = colaboradorColaService.obtenerCola(
                usuario.getId(), filtro, busqueda, pagina, tamanioPagina);

        return ResponseEntity.ok(result);
    }

    /**
     * Actividad del dia del colaborador autenticado.
     * Devuelve los contactos registrados HOY + un resumen por resultado.
     */
    @GetMapping("/mi-actividad")
    public ResponseEntity<?> getMiActividad() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Usuario usuario = usuarioRepository
                .findByUsuarioAndEstado(authentication.getName(), true)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        MiActividadDTO actividad = colaboradorColaService.obtenerActividadHoy(usuario.getId());
        return ResponseEntity.ok(actividad);
    }

    // =========================================================================
    // Estadisticas
    // =========================================================================

    /**
     * Estadisticas del teleoperador autenticado usando enums canonicos.
     */
    @GetMapping("/mis-estadisticas")
    public ResponseEntity<?> getMisEstadisticas() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();

            Usuario usuario = usuarioRepository.findByUsuarioAndEstado(username, true)
                    .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

            Long userId = usuario.getId();

            Map<String, Object> stats = new HashMap<>();
            stats.put("sinGestionar", asignacionRepository.countByUsuario_IdAndEstado(
                    userId, EstadoGestion.SIN_GESTIONAR));
            stats.put("enGestion", asignacionRepository.countByUsuario_IdAndEstado(
                    userId, EstadoGestion.EN_GESTION));
            stats.put("enSeguimiento", asignacionRepository.countByUsuario_IdAndEstado(
                    userId, EstadoGestion.EN_SEGUIMIENTO));
            stats.put("derivados", asignacionRepository.countByUsuario_IdAndEstado(
                    userId, EstadoGestion.DERIVADO));
            stats.put("ganados", asignacionRepository.countByUsuario_IdAndEstado(
                    userId, EstadoGestion.GANADO));
            stats.put("descartados", asignacionRepository.countByUsuario_IdAndEstado(
                    userId, EstadoGestion.DESCARTADO));
            stats.put("noContesto", asignacionRepository.countByUsuario_IdAndEstadoResultado(
                    userId, ResultadoAtencion.NO_CONTESTO));
            stats.put("agendados", asignacionRepository.countByUsuario_IdAndEstadoResultado(
                    userId, ResultadoAtencion.AGENDADO));

            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Error al obtener estadisticas: " + e.getMessage()));
        }
    }

    /**
     * Estadisticas generales de asignaciones.
     */
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @GetMapping("/estadisticas")
    public ResponseEntity<Map<String, Object>> obtenerEstadisticas() {
        try {
            Map<String, Object> estadisticas = asignacionService.obtenerEstadisticas();
            return ResponseEntity.ok(estadisticas);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Error al obtener estadisticas: " + e.getMessage()));
        }
    }
}
