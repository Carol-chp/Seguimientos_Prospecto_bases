package com.pe.swcotoschero.prospectos.Repository;

import com.pe.swcotoschero.prospectos.Entity.Jornada;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface JornadaRepository extends JpaRepository<Jornada, Long> {

    Optional<Jornada> findByUsuario_IdAndFecha(Long usuarioId, LocalDate fecha);

    @Query("SELECT j FROM Jornada j JOIN FETCH j.usuario WHERE j.fecha = :fecha")
    List<Jornada> findByFechaConUsuario(@Param("fecha") LocalDate fecha);
}
