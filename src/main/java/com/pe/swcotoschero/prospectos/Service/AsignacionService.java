package com.pe.swcotoschero.prospectos.Service;

import com.pe.swcotoschero.prospectos.Entity.Asignacion;
import com.pe.swcotoschero.prospectos.Entity.CargaMasiva;
import com.pe.swcotoschero.prospectos.Entity.Prospecto;
import com.pe.swcotoschero.prospectos.Entity.Usuario;
import com.pe.swcotoschero.prospectos.Repository.AsignacionRepository;
import com.pe.swcotoschero.prospectos.Repository.CargaMasivaRepository;
import com.pe.swcotoschero.prospectos.Repository.ProspectoRepository;
import com.pe.swcotoschero.prospectos.Repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class AsignacionService {
    @Autowired
    private AsignacionRepository asignacionRepository;

    @Autowired
    private ProspectoRepository prospectoRepository;

    @Autowired
    private CargaMasivaRepository cargaMasivaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private CargaMasivaService cargaMasivaService;

    public List<Asignacion> listarTodos() {
        return asignacionRepository.findAll();
    }

    public Optional<Asignacion> obtenerPorId(Long id) {
        return asignacionRepository.findById(id);
    }

    public Asignacion guardar(Asignacion asignacion) {
        return asignacionRepository.save(asignacion);
    }

    public void eliminar(Long id) {
        asignacionRepository.deleteById(id);
    }

    /**
     * Asigna prospectos de una carga masiva a un usuario específico.
     * Solo asigna prospectos que aún no tienen asignación.
     * Si se indica cantidad, solo asigna esa cantidad; si no, asigna todos los disponibles.
     *
     * @param cargaMasivaId ID de la carga masiva
     * @param usuarioId ID del usuario al que se asignarán los prospectos
     * @param administradorId ID del administrador que realiza la asignación
     * @param cantidad Cantidad de prospectos a asignar (null = todos los disponibles)
     * @return Map con el resultado de la operación
     */
    @Transactional
    public Map<String, Object> asignarCargaMasivaAUsuario(Long cargaMasivaId, Long usuarioId, Long administradorId, Integer cantidad) {
        // Validar que la carga masiva existe
        CargaMasiva cargaMasiva = cargaMasivaRepository.findById(cargaMasivaId)
                .orElseThrow(() -> new IllegalArgumentException("Carga masiva no encontrada con ID: " + cargaMasivaId));

        // Validar que el usuario existe
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con ID: " + usuarioId));

        // Validar que el administrador existe
        Usuario administrador = usuarioRepository.findById(administradorId)
                .orElseThrow(() -> new IllegalArgumentException("Administrador no encontrado con ID: " + administradorId));

        // Obtener solo prospectos SIN asignación
        List<Prospecto> prospectosSinAsignar = prospectoRepository.findUnassignedByCargaMasiva(cargaMasiva);

        if (prospectosSinAsignar.isEmpty()) {
            throw new IllegalArgumentException("No hay prospectos disponibles para asignar en esta carga masiva");
        }

        // Validar cantidad si se especificó
        if (cantidad != null) {
            if (cantidad <= 0) {
                throw new IllegalArgumentException("La cantidad debe ser mayor a 0");
            }
            if (cantidad > prospectosSinAsignar.size()) {
                throw new IllegalArgumentException("La cantidad solicitada (" + cantidad + ") excede los prospectos disponibles (" + prospectosSinAsignar.size() + ")");
            }
        }

        // Determinar los prospectos a asignar
        List<Prospecto> prospectosAAsignar = cantidad != null
                ? prospectosSinAsignar.stream().limit(cantidad).toList()
                : prospectosSinAsignar;

        // Crear asignaciones en lote
        List<Asignacion> nuevasAsignaciones = prospectosAAsignar.stream()
                .map(prospecto -> {
                    Asignacion asignacion = new Asignacion();
                    asignacion.setProspecto(prospecto);
                    asignacion.setUsuario(usuario);
                    asignacion.setAdministrador(administrador);
                    asignacion.setFechaAsignacion(LocalDateTime.now());
                    asignacion.setEstado("Pendiente");
                    return asignacion;
                })
                .toList();

        // Guardar todas las asignaciones
        if (!nuevasAsignaciones.isEmpty()) {
            asignacionRepository.saveAll(nuevasAsignaciones);
        }

        // Actualizar estado de asignación de la carga masiva
        cargaMasivaService.actualizarEstadoAsignacion(cargaMasivaId);

        // Calcular prospectos restantes sin asignar
        int restantes = prospectosSinAsignar.size() - prospectosAAsignar.size();

        // Preparar respuesta con estadísticas
        Map<String, Object> resultado = new HashMap<>();
        resultado.put("success", true);
        resultado.put("mensaje", "Asignación completada exitosamente");
        resultado.put("cargaMasivaId", cargaMasivaId);
        resultado.put("cargaMasivaNombre", cargaMasiva.getNombrearchivo());
        resultado.put("usuarioId", usuarioId);
        resultado.put("usuarioNombre", usuario.getNombre() + " " + usuario.getApellidos());
        resultado.put("totalProspectos", prospectosSinAsignar.size());
        resultado.put("nuevasAsignaciones", nuevasAsignaciones.size());
        resultado.put("prospectosSinAsignar", restantes);
        resultado.put("fechaAsignacion", LocalDateTime.now());

        return resultado;
    }

    /**
     * Obtiene estadísticas generales de asignaciones
     * 
     * @return Map con las estadísticas
     */
    public Map<String, Object> obtenerEstadisticas() {
        Map<String, Object> estadisticas = new HashMap<>();
        
        // Contar total de asignaciones
        long totalAsignaciones = asignacionRepository.count();
        
        // Contar asignaciones por estado
        long pendientes = asignacionRepository.countByEstado("Pendiente");
        long reasignadas = asignacionRepository.countByEstado("Reasignado");
        long completadas = asignacionRepository.countByEstado("Completado");
        
        // Total de prospectos
        long totalProspectos = prospectoRepository.count();
        
        // Total de cargas masivas
        long totalCargasMasivas = cargaMasivaRepository.count();
        
        // Total de usuarios
        long totalUsuarios = usuarioRepository.count();

        estadisticas.put("totalAsignaciones", totalAsignaciones);
        estadisticas.put("pendientes", pendientes);
        estadisticas.put("reasignadas", reasignadas);
        estadisticas.put("completadas", completadas);
        estadisticas.put("totalProspectos", totalProspectos);
        estadisticas.put("totalCargasMasivas", totalCargasMasivas);
        estadisticas.put("totalUsuarios", totalUsuarios);
        estadisticas.put("porcentajeAsignado", totalProspectos > 0 ? (double) totalAsignaciones / totalProspectos * 100 : 0);

        return estadisticas;
    }
}
