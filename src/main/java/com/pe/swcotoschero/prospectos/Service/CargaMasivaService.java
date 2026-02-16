package com.pe.swcotoschero.prospectos.Service;

import com.pe.swcotoschero.prospectos.Entity.CargaMasiva;
import com.pe.swcotoschero.prospectos.Entity.Usuario;
import com.pe.swcotoschero.prospectos.Repository.AsignacionRepository;
import com.pe.swcotoschero.prospectos.Repository.CargaMasivaRepository;
import com.pe.swcotoschero.prospectos.Repository.ProspectoRepository;
import com.pe.swcotoschero.prospectos.dto.AsignacionResumenDTO;
import com.pe.swcotoschero.prospectos.dto.CargaMasivaDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CargaMasivaService {

    @Autowired
    private CargaMasivaRepository cargaMasivaRepository;

    @Autowired
    private ProspectoRepository prospectoRepository;

    @Autowired
    private AsignacionRepository asignacionRepository;

    /**
     * Listar todas las cargas masivas con información de asignación
     * @return Lista de DTOs con información completa
     */
    public List<CargaMasivaDTO> listarCargasMasivasConInfo() {
        List<CargaMasiva> cargas = cargaMasivaRepository.findAllByOrderByFechaDesc();
        
        return cargas.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Obtener una carga masiva específica por ID
     * @param id ID de la carga masiva
     * @return DTO con información completa
     */
    public CargaMasivaDTO obtenerCargaMasivaPorId(Long id) {
        CargaMasiva carga = cargaMasivaRepository.findById(id).orElse(null);
        return carga != null ? convertToDTO(carga) : null;
    }

    /**
     * Obtener cargas masivas sin asignar
     * @return Lista de cargas sin asignar
     */
    public List<CargaMasivaDTO> obtenerCargasSinAsignar() {
        List<CargaMasiva> cargas = cargaMasivaRepository.findByEstadoAsignacionOrderByFechaDesc("SIN_ASIGNAR");
        
        return cargas.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Obtener cargas masivas asignadas a un usuario específico
     * @param usuarioId ID del usuario
     * @return Lista de cargas asignadas al usuario
     */
    public List<CargaMasivaDTO> obtenerCargasPorUsuario(Long usuarioId) {
        List<CargaMasiva> cargas = cargaMasivaRepository.findByUsuarioAsignado_IdOrderByFechaAsignacionDesc(usuarioId);
        
        return cargas.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Actualizar información de asignación de una carga masiva
     * @param cargaMasivaId ID de la carga masiva
     * @param usuario Usuario asignado
     */
    public void actualizarAsignacion(Long cargaMasivaId, Usuario usuario) {
        CargaMasiva carga = cargaMasivaRepository.findById(cargaMasivaId).orElse(null);
        if (carga != null) {
            carga.setUsuarioAsignado(usuario);
            carga.setEstadoAsignacion("ASIGNADO");
            carga.setFechaAsignacion(java.time.LocalDateTime.now());
            cargaMasivaRepository.save(carga);
        }
    }

    /**
     * Actualizar estado de asignación basado en la cantidad de prospectos asignados
     * @param cargaMasivaId ID de la carga masiva
     */
    public void actualizarEstadoAsignacion(Long cargaMasivaId) {
        CargaMasiva carga = cargaMasivaRepository.findById(cargaMasivaId).orElse(null);
        if (carga != null) {
            Long totalProspectos = prospectoRepository.countByCargaMasiva(carga);
            Long totalAsignados = asignacionRepository.countByCargaMasivaId(cargaMasivaId);

            if (totalAsignados == 0) {
                carga.setEstadoAsignacion("SIN_ASIGNAR");
            } else if (totalAsignados < totalProspectos) {
                carga.setEstadoAsignacion("PARCIALMENTE_ASIGNADO");
            } else {
                carga.setEstadoAsignacion("ASIGNADO");
            }
            carga.setUsuarioAsignado(null);
            carga.setFechaAsignacion(java.time.LocalDateTime.now());
            cargaMasivaRepository.save(carga);
        }
    }

    /**
     * Actualizar cantidad de prospectos de una carga masiva
     * @param cargaMasiva Carga masiva a actualizar
     */
    public void actualizarCantidadProspectos(CargaMasiva cargaMasiva) {
        if (cargaMasiva != null) {
            // Contar prospectos asociados a esta carga masiva
            Long count = prospectoRepository.countByCargaMasiva(cargaMasiva);
            cargaMasiva.setCantidadProspectos(count.intValue());
            cargaMasivaRepository.save(cargaMasiva);
        }
    }

    /**
     * Obtener estadísticas de cargas masivas
     * @return Map con estadísticas
     */
    public Map<String, Object> obtenerEstadisticasCargas() {
        Map<String, Object> estadisticas = new HashMap<>();
        
        // Total de cargas masivas
        long totalCargas = cargaMasivaRepository.count();
        
        // Cargas sin asignar
        long cargasSinAsignar = cargaMasivaRepository.countByEstadoAsignacion("SIN_ASIGNAR");
        
        // Cargas asignadas
        long cargasAsignadas = cargaMasivaRepository.countByEstadoAsignacion("ASIGNADO");
        
        // Total de prospectos en todas las cargas
        Long totalProspectos = cargaMasivaRepository.sumCantidadProspectos();
        
        long cargasParciales = cargaMasivaRepository.countByEstadoAsignacion("PARCIALMENTE_ASIGNADO");

        estadisticas.put("totalCargas", totalCargas);
        estadisticas.put("cargasSinAsignar", cargasSinAsignar);
        estadisticas.put("cargasAsignadas", cargasAsignadas);
        estadisticas.put("cargasParciales", cargasParciales);
        estadisticas.put("totalProspectos", totalProspectos != null ? totalProspectos : 0);
        estadisticas.put("porcentajeAsignado", totalCargas > 0 ? (double) cargasAsignadas / totalCargas * 100 : 0);
        
        return estadisticas;
    }

    /**
     * Convertir entidad CargaMasiva a DTO
     * @param carga Entidad CargaMasiva
     * @return DTO con información completa
     */
    private CargaMasivaDTO convertToDTO(CargaMasiva carga) {
        CargaMasivaDTO dto = new CargaMasivaDTO();

        dto.setId(carga.getId());
        dto.setNombrearchivo(carga.getNombrearchivo());
        dto.setFecha(carga.getFecha());
        dto.setCantidadProspectos(carga.getCantidadProspectos());
        dto.setEstadoAsignacion(carga.getEstadoAsignacion());
        dto.setFechaAsignacion(carga.getFechaAsignacion());

        // Información del usuario asignado (retrocompatibilidad)
        if (carga.getUsuarioAsignado() != null) {
            Usuario usuario = carga.getUsuarioAsignado();
            dto.setUsuarioAsignadoId(usuario.getId());
            dto.setUsuarioAsignadoNombre(usuario.getNombre());
            dto.setUsuarioAsignadoApellidos(usuario.getApellidos());
            dto.setUsuarioAsignadoCompleto(usuario.getNombre() + " " + usuario.getApellidos());
        }

        // Información de asignación parcial
        Long totalAsignados = asignacionRepository.countByCargaMasivaId(carga.getId());
        int asignados = totalAsignados != null ? totalAsignados.intValue() : 0;
        int totalProspectos = carga.getCantidadProspectos() != null ? carga.getCantidadProspectos() : 0;

        dto.setProspectosAsignados(asignados);
        dto.setProspectosSinAsignar(totalProspectos - asignados);

        // Resumen de asignaciones por usuario
        List<Object[]> resumenRaw = asignacionRepository.countAssignmentsByUserForCargaMasiva(carga.getId());
        List<AsignacionResumenDTO> resumen = new ArrayList<>();
        for (Object[] row : resumenRaw) {
            resumen.add(new AsignacionResumenDTO(
                    (Long) row[0],
                    (String) row[1],
                    (String) row[2],
                    (Long) row[3]
            ));
        }
        dto.setResumenAsignaciones(resumen);

        return dto;
    }
}