package com.pe.swcotoschero.prospectos.Service;

import com.pe.swcotoschero.prospectos.Entity.Asignacion;
import com.pe.swcotoschero.prospectos.Entity.Prospecto;
import com.pe.swcotoschero.prospectos.Entity.Usuario;
import com.pe.swcotoschero.prospectos.Entity.enums.EstadoGestion;
import com.pe.swcotoschero.prospectos.Repository.AsignacionRepository;
import com.pe.swcotoschero.prospectos.Repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * RF-23 — reasignación de prospectos y bucket "En riesgo" (5j).
 *
 * - Reasignación manual: el dueño mueve casos ACTIVOS de un colaborador a otro.
 *   Conserva el historial (mismo Asignacion/Contactos; solo cambia el usuario).
 *   Auditoría: reasignadoDeId, fechaReasignacion, motivoReasignacion. NUNCA
 *   se reasigna DERIVADO/GANADO/DESCARTADO.
 * - "En riesgo": casos de colaboradores AUSENTES hoy (SIN_GESTIONAR o
 *   EN_SEGUIMIENTO vencido) — el estado NO cambia (D2); el dueño los reasigna.
 *
 * La atribución de venta (derivadoPor) NO se toca: reasignar solo cambia quién
 * trabaja el caso, no a quién se le acredita una venta futura (D4).
 */
@Service
public class ReasignacionService {

    private static final Set<EstadoGestion> ACTIVOS = EnumSet.of(
            EstadoGestion.SIN_GESTIONAR, EstadoGestion.EN_GESTION,
            EstadoGestion.EN_SEGUIMIENTO);
    private static final long ADMIN_ROL_ID = 1L;

    private final AsignacionRepository asignacionRepository;
    private final UsuarioRepository usuarioRepository;
    private final AsistenciaService asistenciaService;

    public ReasignacionService(AsignacionRepository asignacionRepository,
                               UsuarioRepository usuarioRepository,
                               AsistenciaService asistenciaService) {
        this.asignacionRepository = asignacionRepository;
        this.usuarioRepository = usuarioRepository;
        this.asistenciaService = asistenciaService;
    }

    private Usuario destinoValido(Long nuevoUsuarioId) {
        Usuario u = usuarioRepository.findById(nuevoUsuarioId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Colaborador destino no encontrado: " + nuevoUsuarioId));
        if (!Boolean.TRUE.equals(u.getEstado())) {
            throw new IllegalArgumentException("El colaborador destino está inactivo.");
        }
        if (u.getRol() != null && u.getRol().getId() != null
                && u.getRol().getId() == ADMIN_ROL_ID) {
            throw new IllegalArgumentException(
                    "No se puede reasignar a un administrador.");
        }
        return u;
    }

    @Transactional
    public Map<String, Object> reasignar(List<Long> asignacionIds, Long nuevoUsuarioId,
                                         String motivo, Usuario admin) {
        if (asignacionIds == null || asignacionIds.isEmpty()) {
            throw new IllegalArgumentException("Debe seleccionar al menos un caso a reasignar.");
        }
        Usuario destino = destinoValido(nuevoUsuarioId);
        List<Asignacion> casos = asignacionRepository.findAllById(asignacionIds);
        if (casos.size() != asignacionIds.size()) {
            throw new IllegalArgumentException("Algún caso seleccionado no existe.");
        }
        LocalDateTime ahora = LocalDateTime.now();
        int movidos = 0;
        for (Asignacion a : casos) {
            if (!ACTIVOS.contains(a.getEstado())) {
                throw new IllegalArgumentException(
                        "El caso " + a.getAsignacionID() + " no es reasignable (estado "
                        + a.getEstado() + "). Solo SIN_GESTIONAR/EN_GESTION/EN_SEGUIMIENTO.");
            }
            if (a.getUsuario() != null
                    && a.getUsuario().getId().equals(destino.getId())) {
                continue; // ya pertenece al destino
            }
            a.setReasignadoDeId(a.getUsuario() != null ? a.getUsuario().getId() : null);
            a.setReasignadoParaId(destino.getId());
            a.setUsuario(destino);
            a.setFechaReasignacion(ahora);
            a.setMotivoReasignacion(motivo);
            asignacionRepository.save(a);
            movidos++;
        }
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("ok", true);
        r.put("reasignados", movidos);
        r.put("destinoId", destino.getId());
        r.put("destinoNombre", (destino.getNombre() + " " + destino.getApellidos()).trim());
        return r;
    }

    @Transactional
    public Map<String, Object> reasignarTodoDeColaborador(Long origenUsuarioId,
                                                          Long nuevoUsuarioId,
                                                          String motivo, Usuario admin) {
        List<Asignacion> activas = asignacionRepository.findActivasByUsuario(origenUsuarioId);
        if (activas.isEmpty()) {
            throw new IllegalArgumentException(
                    "El colaborador no tiene casos activos para reasignar.");
        }
        return reasignar(activas.stream().map(Asignacion::getAsignacionID).toList(),
                nuevoUsuarioId, motivo, admin);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> enRiesgo() {
        List<Long> ausentes = asistenciaService.idsAusentesHoy();
        Map<String, Object> r = new LinkedHashMap<>();
        if (ausentes.isEmpty()) {
            r.put("total", 0);
            r.put("resultados", List.of());
            r.put("nota", "No hay colaboradores ausentes hoy (o no es día laborable).");
            return r;
        }
        LocalDateTime finHoy = LocalDate.now().atTime(LocalTime.MAX);
        List<Map<String, Object>> filas = asignacionRepository
                .findEnRiesgo(ausentes, finHoy).stream().map(a -> {
                    Prospecto p = a.getProspecto();
                    Usuario col = a.getUsuario();
                    Map<String, Object> f = new LinkedHashMap<>();
                    f.put("asignacionId", a.getAsignacionID());
                    f.put("prospectoId", p.getProspectoID());
                    f.put("nombre", (p.getNombre() + " " + p.getApellido()).trim());
                    f.put("celular", mask(p.getCelular()));
                    f.put("campania", p.getCampania() != null ? p.getCampania().getNombre() : null);
                    f.put("estado", a.getEstado() != null ? a.getEstado().name() : null);
                    f.put("fechaAgenda", a.getFechaAgenda() != null
                            ? a.getFechaAgenda().toString() : null);
                    f.put("colaboradorAusenteId", col != null ? col.getId() : null);
                    f.put("colaboradorAusente", col != null
                            ? (col.getNombre() + " " + col.getApellidos()).trim() : null);
                    return f;
                }).toList();
        r.put("total", filas.size());
        r.put("resultados", filas);
        return r;
    }

    private static String mask(String v) {
        if (v == null || v.isBlank()) return "";
        String t = v.trim();
        return t.length() <= 3 ? "***" : "*".repeat(t.length() - 3) + t.substring(t.length() - 3);
    }
}
