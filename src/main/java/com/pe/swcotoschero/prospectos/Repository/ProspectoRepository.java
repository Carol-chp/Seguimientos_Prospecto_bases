package com.pe.swcotoschero.prospectos.Repository;

import com.pe.swcotoschero.prospectos.Entity.CargaMasiva;
import com.pe.swcotoschero.prospectos.Entity.Prospecto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    /**
     * Encuentra prospectos de una carga masiva que NO tienen asignación activa.
     *
     * "Sin asignar" = ninguna asignación en estado distinto de DESCARTADO.
     * Solo DESCARTADO libera el prospecto (caso de uso: enviar-banco → DESCARTADO
     * → reasignable en el banco destino). GANADO NO libera: un prospecto cerrado
     * no vuelve al pool de asignación (la recurrencia D7 crea un ciclo nuevo aparte).
     */
    @Query("SELECT p FROM Prospecto p WHERE p.cargaMasiva = :cargaMasiva " +
           "AND NOT EXISTS (" +
           "  SELECT a FROM Asignacion a WHERE a.prospecto = p " +
           "  AND a.estado <> com.pe.swcotoschero.prospectos.Entity.enums.EstadoGestion.DESCARTADO" +
           ")")
    List<Prospecto> findUnassignedByCargaMasiva(CargaMasiva cargaMasiva);

    /**
     * Cuenta prospectos de una carga masiva que NO tienen asignación activa.
     * Misma definición que findUnassignedByCargaMasiva.
     */
    @Query("SELECT COUNT(p) FROM Prospecto p WHERE p.cargaMasiva = :cargaMasiva " +
           "AND NOT EXISTS (" +
           "  SELECT a FROM Asignacion a WHERE a.prospecto = p " +
           "  AND a.estado <> com.pe.swcotoschero.prospectos.Entity.enums.EstadoGestion.DESCARTADO" +
           ")")
    Long countUnassignedByCargaMasiva(CargaMasiva cargaMasiva);

    /**
     * Encuentra prospectos de una carga masiva sin asignación activa y que
     * pertenecen a un banco específico.
     *
     * Usado por AsignacionService para respetar el banco del colaborador destino:
     * solo se asignan prospectos cuyo bancoEntidad coincide con el banco del usuario.
     */
    @Query("SELECT p FROM Prospecto p WHERE p.cargaMasiva = :cargaMasiva " +
           "AND p.bancoEntidad = :banco " +
           "AND NOT EXISTS (" +
           "  SELECT a FROM Asignacion a WHERE a.prospecto = p " +
           "  AND a.estado <> com.pe.swcotoschero.prospectos.Entity.enums.EstadoGestion.DESCARTADO" +
           ")")
    List<Prospecto> findUnassignedByCargaMasivaAndBanco(
            @Param("cargaMasiva") CargaMasiva cargaMasiva,
            @Param("banco") com.pe.swcotoschero.prospectos.Entity.Banco banco);
}

