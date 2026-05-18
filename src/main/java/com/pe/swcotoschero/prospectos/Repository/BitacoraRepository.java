package com.pe.swcotoschero.prospectos.Repository;

import com.pe.swcotoschero.prospectos.Entity.Contacto;
import com.pe.swcotoschero.prospectos.Entity.enums.QuienContesto;
import com.pe.swcotoschero.prospectos.Entity.enums.ResultadoAtencion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

/**
 * RF-20 / §5h — bitácora global de atenciones (todas las atenciones de todos los
 * colaboradores, con filtros). Solo lectura; el dueño audita y consulta.
 *
 * Filtros opcionales (null = sin filtro), mismo patrón que
 * {@code AsignacionRepository.findByUsuarioWithFilters}.
 */
@Repository
public interface BitacoraRepository extends JpaRepository<Contacto, Long> {

    @Query("SELECT c FROM Contacto c " +
           "JOIN c.asignacion a " +
           "JOIN a.prospecto p " +
           "LEFT JOIN p.campania cam " +
           "LEFT JOIN p.cargaMasiva cm " +
           "WHERE (cast(:desde as LocalDateTime) IS NULL OR c.fechaContacto >= :desde) " +
           "AND (cast(:hasta as LocalDateTime) IS NULL OR c.fechaContacto <= :hasta) " +
           "AND (cast(:colaboradorId as Long) IS NULL OR a.usuario.id = :colaboradorId) " +
           "AND (cast(:campania as String) IS NULL OR cam.nombre = :campania) " +
           "AND (cast(:baseId as Long) IS NULL OR cm.id = :baseId) " +
           "AND (:resultado IS NULL OR c.estadoResultado = :resultado) " +
           "AND (:quienContesto IS NULL OR c.quienContesto = :quienContesto) " +
           "ORDER BY c.fechaContacto DESC")
    Page<Contacto> buscar(
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta,
            @Param("colaboradorId") Long colaboradorId,
            @Param("campania") String campania,
            @Param("baseId") Long baseId,
            @Param("resultado") ResultadoAtencion resultado,
            @Param("quienContesto") QuienContesto quienContesto,
            Pageable pageable);
}
