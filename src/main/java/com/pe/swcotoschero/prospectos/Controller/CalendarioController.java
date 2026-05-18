package com.pe.swcotoschero.prospectos.Controller;

import com.pe.swcotoschero.prospectos.Entity.CalendarioLaboral;
import com.pe.swcotoschero.prospectos.Repository.CalendarioLaboralRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

/** RF-22 — calendario laboral (feriados). CRUD solo ADMINISTRADOR. */
@RestController
@RequestMapping("/api/calendario")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class CalendarioController {

    private final CalendarioLaboralRepository repo;

    public CalendarioController(CalendarioLaboralRepository repo) {
        this.repo = repo;
    }

    /** Lista feriados; opcional ?anio=2026 filtra por año. */
    @GetMapping
    public ResponseEntity<List<CalendarioLaboral>> listar(
            @RequestParam(required = false) Integer anio) {
        if (anio != null) {
            return ResponseEntity.ok(repo.findByFechaBetweenOrderByFechaAsc(
                    LocalDate.of(anio, 1, 1), LocalDate.of(anio, 12, 31)));
        }
        return ResponseEntity.ok(repo.findAllByOrderByFechaAsc());
    }

    /** Agrega un feriado. Body: { "fecha":"YYYY-MM-DD", "descripcion":"..." }. */
    @PostMapping
    public ResponseEntity<CalendarioLaboral> agregar(@RequestBody Map<String, String> body) {
        String fechaStr = body.get("fecha");
        if (fechaStr == null || fechaStr.isBlank()) {
            throw new IllegalArgumentException("La fecha es obligatoria (YYYY-MM-DD).");
        }
        LocalDate fecha;
        try {
            fecha = LocalDate.parse(fechaStr.trim());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Fecha inválida (use YYYY-MM-DD): " + fechaStr);
        }
        if (repo.findByFecha(fecha).isPresent()) {
            throw new IllegalArgumentException("Ya existe un registro para " + fecha + ".");
        }
        CalendarioLaboral c = new CalendarioLaboral();
        c.setFecha(fecha);
        c.setEsFeriado(true);
        c.setDescripcion(body.getOrDefault("descripcion", "Feriado"));
        return ResponseEntity.ok(repo.save(c));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> eliminar(@PathVariable Long id) {
        if (!repo.existsById(id)) {
            throw new IllegalArgumentException("Feriado no encontrado: " + id);
        }
        repo.deleteById(id);
        return ResponseEntity.ok(Map.of("ok", true));
    }
}
