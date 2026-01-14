package com.pe.swcotoschero.prospectos.Repository;

import com.pe.swcotoschero.prospectos.Entity.Asignacion;
import com.pe.swcotoschero.prospectos.Entity.Prospecto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AsignacionRepository extends JpaRepository<Asignacion, Long> {
    Optional<Asignacion> findByProspecto_ProspectoID(Long prospectoId);
    
    /**
     * Encuentra todas las asignaciones para una lista de prospectos
     */
    List<Asignacion> findByProspectoIn(List<Prospecto> prospectos);
    
    /**
     * Cuenta asignaciones por estado
     */
    long countByEstado(String estado);
}
