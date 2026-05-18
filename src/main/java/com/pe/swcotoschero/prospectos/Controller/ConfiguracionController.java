package com.pe.swcotoschero.prospectos.Controller;

import com.pe.swcotoschero.prospectos.Entity.ConfiguracionDueno;
import com.pe.swcotoschero.prospectos.Repository.ConfiguracionDuenoRepository;
import com.pe.swcotoschero.prospectos.dto.ConfiguracionRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Configuración del dueño (RF-08 toggles, metas, parámetros 6b.1). Solo ADMINISTRADOR.
 * Singleton: una sola fila (seed en V1).
 */
@RestController
@RequestMapping("/api/reportes/config")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class ConfiguracionController {

    private final ConfiguracionDuenoRepository repo;

    public ConfiguracionController(ConfiguracionDuenoRepository repo) {
        this.repo = repo;
    }

    private ConfiguracionDueno cargar() {
        return repo.findTopByOrderByIdAsc().orElseThrow(() ->
                new IllegalStateException("No existe configuración del dueño (seed faltante)."));
    }

    @GetMapping
    public ResponseEntity<ConfiguracionDueno> obtener() {
        return ResponseEntity.ok(cargar());
    }

    /** Patch parcial: solo los campos no-null se actualizan. */
    @PutMapping
    public ResponseEntity<ConfiguracionDueno> actualizar(@RequestBody ConfiguracionRequest req) {
        ConfiguracionDueno c = cargar();
        if (req.getToggleEmailInstantaneo() != null) c.setToggleEmailInstantaneo(req.getToggleEmailInstantaneo());
        if (req.getToggleEmailDigest() != null) c.setToggleEmailDigest(req.getToggleEmailDigest());
        if (req.getToggleResumenDiario() != null) c.setToggleResumenDiario(req.getToggleResumenDiario());
        if (req.getMetaVentasMensual() != null) c.setMetaVentasMensual(req.getMetaVentasMensual());
        if (req.getMetaDerivadosPorColaborador() != null)
            c.setMetaDerivadosPorColaborador(req.getMetaDerivadosPorColaborador());
        if (req.getPlazoReevaluacionSbsDias() != null) {
            if (req.getPlazoReevaluacionSbsDias() < 1)
                throw new IllegalArgumentException("plazoReevaluacionSbsDias debe ser >= 1.");
            c.setPlazoReevaluacionSbsDias(req.getPlazoReevaluacionSbsDias());
        }
        if (req.getMaxIntentosNoContesto() != null) {
            if (req.getMaxIntentosNoContesto() < 1)
                throw new IllegalArgumentException("maxIntentosNoContesto debe ser >= 1.");
            c.setMaxIntentosNoContesto(req.getMaxIntentosNoContesto());
        }
        if (req.getReglaReintentoNoContesto() != null)
            c.setReglaReintentoNoContesto(req.getReglaReintentoNoContesto());
        if (req.getHoraInicioJornada() != null) c.setHoraInicioJornada(req.getHoraInicioJornada());
        if (req.getMinutosGraciaAusencia() != null) {
            if (req.getMinutosGraciaAusencia() < 0)
                throw new IllegalArgumentException("minutosGraciaAusencia debe ser >= 0.");
            c.setMinutosGraciaAusencia(req.getMinutosGraciaAusencia());
        }
        return ResponseEntity.ok(repo.save(c));
    }
}
