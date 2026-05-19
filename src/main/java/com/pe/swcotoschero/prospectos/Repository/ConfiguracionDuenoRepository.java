package com.pe.swcotoschero.prospectos.Repository;

import com.pe.swcotoschero.prospectos.Entity.ConfiguracionDueno;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Repositorio de la configuracion singleton del dueno.
 * La tabla tiene exactamente un registro; se accede con findTopByOrderByIdAsc().
 */
@Repository
public interface ConfiguracionDuenoRepository extends JpaRepository<ConfiguracionDueno, Long> {

    Optional<ConfiguracionDueno> findTopByOrderByIdAsc();

    /**
     * Reclama atómicamente el envío del resumen del día (deduplicación entre
     * réplicas). UPDATE condicional: solo actualiza si aún NO se envió/intentó
     * hoy. En Postgres el lock de fila serializa las réplicas que disparan en
     * el mismo segundo → exactamente UNA recibe 1 (gana el envío); el resto
     * recibe 0 y debe saltar. El estado final lo reescribe registrarEstado().
     */
    @Transactional  // tx propia: el caller (enviarResumenDiarioAsync→this.enviarResumenDiario)
                    // pierde el @Transactional por auto-invocación; el UPDATE la necesita.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE ConfiguracionDueno c SET c.ultimoEnvioResumenFecha = :ahora " +
           "WHERE c.id = :id AND (c.ultimoEnvioResumenFecha IS NULL " +
           "OR c.ultimoEnvioResumenFecha < :inicioHoy)")
    int reclamarEnvioDelDia(@Param("id") Long id,
                            @Param("ahora") LocalDateTime ahora,
                            @Param("inicioHoy") LocalDateTime inicioHoy);
}
