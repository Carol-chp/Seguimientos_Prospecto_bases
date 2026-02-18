package com.pe.swcotoschero.prospectos.Repository;

import com.pe.swcotoschero.prospectos.Entity.Contacto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface ContactoRepository extends JpaRepository<Contacto, Long> {

    /**
     * Cuenta contactos por asignacion
     */
    long countByAsignacion_AsignacionID(Integer asignacionId);

    /**
     * Encuentra el ultimo contacto de una asignacion
     */
    Optional<Contacto> findTopByAsignacion_AsignacionIDOrderByFechaContactoDesc(Integer asignacionId);
}
