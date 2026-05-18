package com.pe.swcotoschero.prospectos.Service;

import com.pe.swcotoschero.prospectos.Entity.Asignacion;
import com.pe.swcotoschero.prospectos.Entity.ConfiguracionDueno;
import com.pe.swcotoschero.prospectos.Entity.enums.EstadoGestion;
import com.pe.swcotoschero.prospectos.Entity.enums.ResultadoAtencion;
import com.pe.swcotoschero.prospectos.Repository.AsignacionRepository;
import com.pe.swcotoschero.prospectos.Repository.ConfiguracionDuenoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Job diario (1.7 / D2 / D7).
 *
 * D2: la cola de EN_SEGUIMIENTO se calcula POR FECHA — el job NO la reactiva
 *     ni cambia su estado. Solo hace transiciones reales:
 *  - D7: GANADO con fechaElegibilidad vencida → crea un CICLO NUEVO
 *        (SIN_GESTIONAR, re-verifica SBS, colaborador original); el GANADO
 *        queda intacto (venta histórica inmutable) y se limpia su
 *        fechaElegibilidad para no reprocesarlo.
 *  - Red de seguridad: NO_CONTESTO que superó el máximo de intentos y quedó
 *        sin descartar → DESCARTADO/ILOCALIZABLE.
 *
 * Detección de ausencia / "En riesgo" → Fase 2 (no en MVP).
 */
@Service
public class JobDiarioService {

    private static final Logger log = LoggerFactory.getLogger(JobDiarioService.class);

    private final AsignacionRepository asignacionRepository;
    private final ConfiguracionDuenoRepository configRepo;

    public JobDiarioService(AsignacionRepository asignacionRepository,
                            ConfiguracionDuenoRepository configRepo) {
        this.asignacionRepository = asignacionRepository;
        this.configRepo = configRepo;
    }

    @Transactional
    public Map<String, Object> ejecutar() {
        LocalDate hoy = LocalDate.now();
        int ciclosNuevos = reactivarGanadosReelegibles(hoy);
        int ilocalizables = descartarIlocalizables();

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("fecha", LocalDateTime.now().toString());
        r.put("ciclosNuevosD7", ciclosNuevos);
        r.put("ilocalizablesDescartados", ilocalizables);
        log.info("Job diario: {} ciclos nuevos D7, {} ilocalizables descartados",
                ciclosNuevos, ilocalizables);
        return r;
    }

    private int reactivarGanadosReelegibles(LocalDate hoy) {
        List<Asignacion> ganados = asignacionRepository.findGanadosReelegibles(hoy);
        int creados = 0;
        for (Asignacion g : ganados) {
            boolean colaboradorActivo = g.getUsuario() != null
                    && Boolean.TRUE.equals(g.getUsuario().getEstado());
            if (!colaboradorActivo) {
                // D3: el colaborador original ya no está → debería ir al pool
                // "Por reasignar" (Fase 2). En MVP se crea igual asignado al
                // original y se deja traza para reasignación manual.
                log.warn("D7: colaborador del ciclo {} inactivo; ciclo nuevo creado "
                        + "igualmente (reasignación manual — pool es Fase 2)",
                        g.getAsignacionID());
            }

            Asignacion nuevo = new Asignacion();
            nuevo.setProspecto(g.getProspecto());
            nuevo.setUsuario(g.getUsuario());
            nuevo.setAdministrador(g.getAdministrador());
            nuevo.setAsignadoPor(g.getAdministrador());
            nuevo.setEstado(EstadoGestion.SIN_GESTIONAR);
            nuevo.setFechaAsignacion(LocalDateTime.now());
            nuevo.setFechaAsignacionRegistro(LocalDateTime.now());
            nuevo.setCicloAnteriorId(g.getAsignacionID());
            nuevo.setIntentosFallidos(0);
            // Préstamo nuevo = nueva evaluación: SBS se re-verifica (queda null).
            asignacionRepository.save(nuevo);

            // El GANADO histórico NO se muta; solo se evita reprocesarlo.
            g.setFechaElegibilidad(null);
            asignacionRepository.save(g);
            creados++;
        }
        return creados;
    }

    private int descartarIlocalizables() {
        ConfiguracionDueno cfg = configRepo.findTopByOrderByIdAsc()
                .orElseGet(ConfiguracionDueno::new);
        int max = cfg.getMaxIntentosNoContesto() != null ? cfg.getMaxIntentosNoContesto() : 6;

        List<Asignacion> pendientes = asignacionRepository.findIlocalizablesPendientes(max);
        for (Asignacion a : pendientes) {
            a.setEstado(EstadoGestion.DESCARTADO);
            a.setEstadoResultado(ResultadoAtencion.ILOCALIZABLE);
            a.setFechaAgenda(null);
            asignacionRepository.save(a);
        }
        return pendientes.size();
    }
}
