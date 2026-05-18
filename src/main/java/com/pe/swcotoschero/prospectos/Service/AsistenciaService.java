package com.pe.swcotoschero.prospectos.Service;

import com.pe.swcotoschero.prospectos.Entity.ConfiguracionDueno;
import com.pe.swcotoschero.prospectos.Entity.Jornada;
import com.pe.swcotoschero.prospectos.Entity.Usuario;
import com.pe.swcotoschero.prospectos.Repository.CalendarioLaboralRepository;
import com.pe.swcotoschero.prospectos.Repository.ConfiguracionDuenoRepository;
import com.pe.swcotoschero.prospectos.Repository.JornadaRepository;
import com.pe.swcotoschero.prospectos.Repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * RF-22 — días laborables y detección de ausencia.
 *
 * Día laborable (decisión 6b.1) = Lunes a Sábado, NO domingo, y NO feriado del
 * calendario. La ausencia SOLO se evalúa en días laborables, y solo después de
 * (horaInicioJornada + minutosGraciaAusencia) — config del dueño (def 09:00+45).
 */
@Service
public class AsistenciaService {

    private static final long ADMIN_ROL_ID = 1L;

    private final UsuarioRepository usuarioRepository;
    private final JornadaRepository jornadaRepository;
    private final CalendarioLaboralRepository calendarioRepository;
    private final ConfiguracionDuenoRepository configRepo;

    public AsistenciaService(UsuarioRepository usuarioRepository,
                             JornadaRepository jornadaRepository,
                             CalendarioLaboralRepository calendarioRepository,
                             ConfiguracionDuenoRepository configRepo) {
        this.usuarioRepository = usuarioRepository;
        this.jornadaRepository = jornadaRepository;
        this.calendarioRepository = calendarioRepository;
        this.configRepo = configRepo;
    }

    public boolean esDiaLaborable(LocalDate fecha) {
        if (fecha.getDayOfWeek() == DayOfWeek.SUNDAY) return false;
        return !calendarioRepository.existsByFechaAndEsFeriadoTrue(fecha);
    }

    private LocalDateTime limiteAusencia(LocalDate fecha, ConfiguracionDueno cfg) {
        LocalTime inicio;
        try {
            inicio = LocalTime.parse(cfg.getHoraInicioJornada()); // "09:00"
        } catch (Exception e) {
            inicio = LocalTime.of(9, 0);
        }
        int gracia = cfg.getMinutosGraciaAusencia() != null
                ? cfg.getMinutosGraciaAusencia() : 45;
        return fecha.atTime(inicio).plusMinutes(gracia);
    }

    /** Resumen de asistencia de hoy (lo consume el dashboard/email en 2.4). */
    @Transactional(readOnly = true)
    public Map<String, Object> asistenciaHoy() {
        LocalDate hoy = LocalDate.now();
        ConfiguracionDueno cfg = configRepo.findTopByOrderByIdAsc()
                .orElseGet(ConfiguracionDueno::new);
        boolean laborable = esDiaLaborable(hoy);
        LocalDateTime limite = limiteAusencia(hoy, cfg);
        boolean pasoLimite = LocalDateTime.now().isAfter(limite);

        List<Usuario> colaboradores =
                usuarioRepository.findActiveUsersWithoutAdminRole(ADMIN_ROL_ID);

        List<Map<String, Object>> filas = new ArrayList<>();
        int ausentes = 0;
        for (Usuario u : colaboradores) {
            Jornada j = jornadaRepository
                    .findByUsuario_IdAndFecha(u.getId(), hoy).orElse(null);
            boolean iniciada = j != null && j.getInicio() != null;
            // Ausente solo si: día laborable + pasó el límite + no inició jornada
            boolean ausente = laborable && pasoLimite && !iniciada;
            if (ausente) ausentes++;

            Map<String, Object> fila = new LinkedHashMap<>();
            fila.put("usuarioId", u.getId());
            fila.put("nombre", (u.getNombre() + " " + u.getApellidos()).trim());
            fila.put("jornadaIniciada", iniciada);
            fila.put("inicio", j != null && j.getInicio() != null
                    ? j.getInicio().toString() : null);
            fila.put("fin", j != null && j.getFin() != null
                    ? j.getFin().toString() : null);
            fila.put("ausente", ausente);
            filas.add(fila);
        }

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("fecha", hoy.toString());
        r.put("esLaborable", laborable);
        r.put("limiteAusencia", limite.toString());
        r.put("totalColaboradores", colaboradores.size());
        r.put("totalAusentes", ausentes);
        r.put("colaboradores", filas);
        return r;
    }

    /** IDs de colaboradores ausentes hoy (usado por la reasignación automática 2.3). */
    @Transactional(readOnly = true)
    public List<Long> idsAusentesHoy() {
        List<Long> ids = new ArrayList<>();
        Object filas = asistenciaHoy().get("colaboradores");
        if (filas instanceof List<?> lista) {
            for (Object o : lista) {
                if (o instanceof Map<?, ?> m && Boolean.TRUE.equals(m.get("ausente"))) {
                    ids.add((Long) m.get("usuarioId"));
                }
            }
        }
        return ids;
    }
}
