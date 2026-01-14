package com.pe.swcotoschero.prospectos.Controller;

import com.pe.swcotoschero.prospectos.Entity.CargaMasiva;
import com.pe.swcotoschero.prospectos.Service.CargaMasivaService;
import com.pe.swcotoschero.prospectos.dto.CargaMasivaDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cargas-masivas")
public class CargaMasivaController {

    @Autowired
    private CargaMasivaService cargaMasivaService;

    /**
     * Listar todas las cargas masivas con información de asignación
     * @return Lista de cargas masivas con detalles
     */
    @GetMapping
    public ResponseEntity<List<CargaMasivaDTO>> listarCargasMasivas() {
        try {
            List<CargaMasivaDTO> cargas = cargaMasivaService.listarCargasMasivasConInfo();
            return ResponseEntity.ok(cargas);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Obtener una carga masiva específica por ID
     * @param id ID de la carga masiva
     * @return Detalles de la carga masiva
     */
    @GetMapping("/{id}")
    public ResponseEntity<CargaMasivaDTO> obtenerCargaMasiva(@PathVariable Long id) {
        try {
            CargaMasivaDTO carga = cargaMasivaService.obtenerCargaMasivaPorId(id);
            if (carga != null) {
                return ResponseEntity.ok(carga);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Obtener cargas masivas sin asignar
     * @return Lista de cargas masivas sin asignar
     */
    @GetMapping("/sin-asignar")
    public ResponseEntity<List<CargaMasivaDTO>> obtenerCargasSinAsignar() {
        try {
            List<CargaMasivaDTO> cargas = cargaMasivaService.obtenerCargasSinAsignar();
            return ResponseEntity.ok(cargas);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Obtener cargas masivas asignadas a un usuario específico
     * @param usuarioId ID del usuario
     * @return Lista de cargas masivas asignadas al usuario
     */
    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<List<CargaMasivaDTO>> obtenerCargasPorUsuario(@PathVariable Long usuarioId) {
        try {
            List<CargaMasivaDTO> cargas = cargaMasivaService.obtenerCargasPorUsuario(usuarioId);
            return ResponseEntity.ok(cargas);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Obtener estadísticas de cargas masivas
     * @return Estadísticas generales
     */
    @GetMapping("/estadisticas")
    public ResponseEntity<Map<String, Object>> obtenerEstadisticas() {
        try {
            Map<String, Object> estadisticas = cargaMasivaService.obtenerEstadisticasCargas();
            return ResponseEntity.ok(estadisticas);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}