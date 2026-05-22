package com.pe.swcotoschero.prospectos.Repository;

import com.pe.swcotoschero.prospectos.Entity.Banco;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BancoRepository extends JpaRepository<Banco, Long> {

    /** Banco marcado como default (para asignar bases importadas). Debe existir exactamente uno. */
    Optional<Banco> findFirstByEsDefaultTrue();

    /** Lista de bancos activos ordenados por nombre, para selects y catálogos. */
    List<Banco> findByActivoTrueOrderByNombreAsc();

    /** Verificar existencia por nombre (validación de unicidad). */
    boolean existsByNombreIgnoreCase(String nombre);

    /** Buscar por nombre exacto (insensible a mayúsculas) para detectar duplicados al editar. */
    Optional<Banco> findByNombreIgnoreCase(String nombre);
}
