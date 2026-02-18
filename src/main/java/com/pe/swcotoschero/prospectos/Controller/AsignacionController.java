package com.pe.swcotoschero.prospectos.Controller;
import com.pe.swcotoschero.prospectos.Entity.Asignacion;
import com.pe.swcotoschero.prospectos.Entity.Contacto;
import com.pe.swcotoschero.prospectos.Entity.Usuario;
import com.pe.swcotoschero.prospectos.Repository.AsignacionRepository;
import com.pe.swcotoschero.prospectos.Repository.ContactoRepository;
import com.pe.swcotoschero.prospectos.Repository.UsuarioRepository;
import com.pe.swcotoschero.prospectos.Service.AsignacionService;
import com.pe.swcotoschero.prospectos.dto.MiProspectoDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/asignaciones")
public class AsignacionController {

    @Autowired
    private AsignacionService asignacionService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private AsignacionRepository asignacionRepository;

    @Autowired
    private ContactoRepository contactoRepository;

    /**
     * Asignar prospectos de una carga masiva a un usuario
     */
    @PostMapping("/asignar-carga-masiva")
    public ResponseEntity<?> asignarCargaMasivaAUsuario(
            @RequestParam Long cargaMasivaId,
            @RequestParam Long usuarioId,
            @RequestParam(required = false) Integer cantidad) {

        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();

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
     * Obtener prospectos asignados al usuario autenticado
     */
    @GetMapping("/mis-prospectos")
    public ResponseEntity<?> getMisProspectos(
            @RequestParam(defaultValue = "1") int pagina,
            @RequestParam(defaultValue = "10") int tamanioPagina,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String estadoResultado) {

        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();

            Usuario usuario = usuarioRepository.findByUsuarioAndEstado(username, true)
                    .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

            PageRequest pageRequest = PageRequest.of(pagina > 0 ? pagina - 1 : 0, tamanioPagina);
            Page<Asignacion> page = asignacionRepository.findByUsuarioWithFilters(
                    usuario.getId(),
                    estado,
                    estadoResultado,
                    pageRequest);

            List<MiProspectoDTO> prospectos = page.getContent().stream()
                    .map(asignacion -> {
                        Optional<Contacto> ultimoContacto = contactoRepository
                                .findTopByAsignacion_AsignacionIDOrderByFechaContactoDesc(asignacion.getAsignacionID());
                        long totalContactos = contactoRepository
                                .countByAsignacion_AsignacionID(asignacion.getAsignacionID());

                        return MiProspectoDTO.builder()
                                .prospectoId(asignacion.getProspecto().getProspectoID())
                                .nombre(asignacion.getProspecto().getNombre())
                                .apellido(asignacion.getProspecto().getApellido())
                                .celular(asignacion.getProspecto().getCelular())
                                .documentoIdentidad(asignacion.getProspecto().getDocumentoIdentidad())
                                .campania(asignacion.getProspecto().getCampania() != null
                                        ? asignacion.getProspecto().getCampania().getNombre() : null)
                                .estado(asignacion.getEstado())
                                .estadoResultado(asignacion.getEstadoResultado())
                                .fechaAgenda(asignacion.getFechaAgenda())
                                .ultimoContacto(ultimoContacto.map(Contacto::getFechaContacto).orElse(null))
                                .totalContactos((int) totalContactos)
                                .build();
                    })
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("resultados", prospectos);
            response.put("pagina", page.getNumber() + 1);
            response.put("tamanioPagina", page.getSize());
            response.put("total", page.getTotalElements());
            response.put("totalPaginas", page.getTotalPages());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Error al obtener prospectos: " + e.getMessage()));
        }
    }

    /**
     * Obtener estadisticas del teleoperador autenticado
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
            stats.put("sinGestionar", asignacionRepository.countByUsuario_IdAndEstado(userId, "SIN_GESTIONAR"));
            stats.put("enGestion", asignacionRepository.countByUsuario_IdAndEstado(userId, "EN_GESTION"));
            stats.put("finalizados", asignacionRepository.countByUsuario_IdAndEstado(userId, "FINALIZADO"));
            stats.put("agendados", asignacionRepository.countByUsuario_IdAndEstadoResultado(userId, "AGENDADO"));
            stats.put("noContesto", asignacionRepository.countByUsuario_IdAndEstadoResultado(userId, "NO_CONTESTO"));
            stats.put("prospectos", asignacionRepository.countByUsuario_IdAndEstadoResultado(userId, "PROSPECTO"));
            stats.put("concretos", asignacionRepository.countByUsuario_IdAndEstadoResultado(userId, "CONCRETO_PRESTAMO"));

            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Error al obtener estadísticas: " + e.getMessage()));
        }
    }

    /**
     * Obtener estadísticas generales de asignaciones
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
