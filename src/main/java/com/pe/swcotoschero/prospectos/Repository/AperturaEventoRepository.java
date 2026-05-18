package com.pe.swcotoschero.prospectos.Repository;

import com.pe.swcotoschero.prospectos.Entity.AperturaEvento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio para eventos de apertura del modal de atencion (RF-14).
 */
@Repository
public interface AperturaEventoRepository extends JpaRepository<AperturaEvento, Long> {
}
