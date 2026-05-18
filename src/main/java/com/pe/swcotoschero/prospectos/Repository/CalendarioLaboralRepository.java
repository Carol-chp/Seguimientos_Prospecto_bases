package com.pe.swcotoschero.prospectos.Repository;

import com.pe.swcotoschero.prospectos.Entity.CalendarioLaboral;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CalendarioLaboralRepository extends JpaRepository<CalendarioLaboral, Long> {

    boolean existsByFechaAndEsFeriadoTrue(LocalDate fecha);

    Optional<CalendarioLaboral> findByFecha(LocalDate fecha);

    List<CalendarioLaboral> findByFechaBetweenOrderByFechaAsc(LocalDate desde, LocalDate hasta);

    List<CalendarioLaboral> findAllByOrderByFechaAsc();
}
