package com.pe.swcotoschero.prospectos.Repository;

import com.pe.swcotoschero.prospectos.Entity.ConfiguracionDueno;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio de la configuracion singleton del dueno.
 * La tabla tiene exactamente un registro; se accede con findTopByOrderByIdAsc().
 */
@Repository
public interface ConfiguracionDuenoRepository extends JpaRepository<ConfiguracionDueno, Long> {

    Optional<ConfiguracionDueno> findTopByOrderByIdAsc();
}
