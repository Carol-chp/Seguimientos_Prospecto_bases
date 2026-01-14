package com.pe.swcotoschero.prospectos.Repository;

import com.pe.swcotoschero.prospectos.Entity.CargaMasiva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CargaMasivaRepository extends JpaRepository<CargaMasiva, Long> {
    
    /**
     * Buscar todas las cargas masivas ordenadas por fecha descendente
     */
    List<CargaMasiva> findAllByOrderByFechaDesc();
    
    /**
     * Buscar cargas masivas por estado de asignación
     */
    List<CargaMasiva> findByEstadoAsignacionOrderByFechaDesc(String estadoAsignacion);
    
    /**
     * Buscar cargas masivas asignadas a un usuario específico
     */
    List<CargaMasiva> findByUsuarioAsignado_IdOrderByFechaAsignacionDesc(Long usuarioId);
    
    /**
     * Contar cargas masivas por estado de asignación
     */
    long countByEstadoAsignacion(String estadoAsignacion);
    
    /**
     * Sumar la cantidad total de prospectos en todas las cargas masivas
     */
    @Query("SELECT SUM(c.cantidadProspectos) FROM CargaMasiva c")
    Long sumCantidadProspectos();
}
