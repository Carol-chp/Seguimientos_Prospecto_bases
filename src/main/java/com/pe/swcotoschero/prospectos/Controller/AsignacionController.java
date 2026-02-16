package com.pe.swcotoschero.prospectos.Controller;
import com.pe.swcotoschero.prospectos.Entity.Usuario;
import com.pe.swcotoschero.prospectos.Repository.UsuarioRepository;
import com.pe.swcotoschero.prospectos.Service.AsignacionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/asignaciones")
public class AsignacionController {

    @Autowired
    private AsignacionService asignacionService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    /**
     * Asignar todos los prospectos de una carga masiva a un usuario
     * @param cargaMasivaId ID de la carga masiva
     * @param usuarioId ID del usuario al que se asignarán los prospectos
     * @return ResponseEntity con el resultado de la operación
     */
    @PostMapping("/asignar-carga-masiva")
    public ResponseEntity<?> asignarCargaMasivaAUsuario(
            @RequestParam Long cargaMasivaId,
            @RequestParam Long usuarioId,
            @RequestParam(required = false) Integer cantidad) {

        try {
            // Obtener el usuario autenticado desde el contexto de seguridad
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();

            // Buscar el usuario autenticado para obtener su ID
            Usuario usuarioAutenticado = usuarioRepository.findByUsuarioAndEstado(username, true)
                    .orElseThrow(() -> new IllegalArgumentException("Usuario autenticado no encontrado"));

            Map<String, Object> resultado = asignacionService.asignarCargaMasivaAUsuario(cargaMasivaId, usuarioId, usuarioAutenticado.getId(), cantidad);
            return ResponseEntity.ok(resultado);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Error interno del servidor: " + e.getMessage()));
        }
    }

    /**
     * Obtener estadísticas de asignaciones
     * @return ResponseEntity con las estadísticas
     */
    @GetMapping("/estadisticas")
    public ResponseEntity<Map<String, Object>> obtenerEstadisticas() {
        try {
            Map<String, Object> estadisticas = asignacionService.obtenerEstadisticas();
            return ResponseEntity.ok(estadisticas);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Error al obtener estadísticas: " + e.getMessage()));
        }
    }
}
