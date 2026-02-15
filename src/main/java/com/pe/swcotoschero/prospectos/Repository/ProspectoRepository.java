package com.pe.swcotoschero.prospectos.Repository;

import com.pe.swcotoschero.prospectos.Entity.CargaMasiva;
import com.pe.swcotoschero.prospectos.Entity.Prospecto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProspectoRepository extends JpaRepository<Prospecto, Long> {
    List<Prospecto> findByCampaniaNombreAndDistrito(String campania, String distrito);

    @Query("select p from Prospecto p where (:campania = '' or p.campania.nombre ilike %:campania%)" +
            " and (:filtro = '' or p.nombre ilike %:filtro% or p.apellido ilike %:filtro% or p.celular ilike %:filtro% or p.documentoIdentidad ilike %:filtro%)")
    Page<Prospecto> findProspectos(String campania, String filtro, Pageable pageable);
    
    @Query("select p from Prospecto p where p.estadoInteresado = true")
    Page<Prospecto> findProspectosInteresados(Pageable pageable);
    
    @Query("select p from Prospecto p where p.estadoInteresado = true and (:campania = '' or p.campania.nombre ilike %:campania%)" +
            " and (:filtro = '' or p.nombre ilike %:filtro% or p.apellido ilike %:filtro% or p.celular ilike %:filtro% or p.documentoIdentidad ilike %:filtro%)")
    Page<Prospecto> findProspectosInteresados(String campania, String filtro, Pageable pageable);
    
    /**
     * Encuentra todos los prospectos de una carga masiva específica
     */
    List<Prospecto> findByCargaMasiva(CargaMasiva cargaMasiva);
    
    /**
     * Cuenta los prospectos de una carga masiva específica
     */
    Long countByCargaMasiva(CargaMasiva cargaMasiva);
}

