package com.pe.swcotoschero.prospectos.Repository;

import com.pe.swcotoschero.prospectos.Entity.Asignacion;
import com.pe.swcotoschero.prospectos.Entity.Prospecto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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

    /**
     * Verifica si existen asignaciones para un usuario (como personal o administrador)
     */
    boolean existsByUsuario_Id(Long usuarioId);
    boolean existsByAdministrador_Id(Long administradorId);

    /**
     * Cuenta asignaciones agrupadas por usuario para una carga masiva
     * Retorna [usuarioId, nombre, apellidos, count]
     */
    @Query("SELECT a.usuario.id, a.usuario.nombre, a.usuario.apellidos, COUNT(a) " +
           "FROM Asignacion a WHERE a.prospecto.cargaMasiva.id = :cargaMasivaId " +
           "GROUP BY a.usuario.id, a.usuario.nombre, a.usuario.apellidos")
    List<Object[]> countAssignmentsByUserForCargaMasiva(Long cargaMasivaId);

    /**
     * Cuenta total de asignaciones para una carga masiva
     */
    @Query("SELECT COUNT(a) FROM Asignacion a WHERE a.prospecto.cargaMasiva.id = :cargaMasivaId")
    Long countByCargaMasivaId(Long cargaMasivaId);
}
