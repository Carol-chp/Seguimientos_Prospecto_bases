package com.pe.swcotoschero.prospectos.Repository;

import com.pe.swcotoschero.prospectos.Entity.Contacto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ContactoRepository extends JpaRepository<Contacto, Long> {

    /** Cuenta contactos por asignacion. */
    long countByAsignacion_AsignacionID(Long asignacionId);

    /** Encuentra el ultimo contacto de una asignacion ordenado por fecha descendente. */
    Optional<Contacto> findTopByAsignacion_AsignacionIDOrderByFechaContactoDesc(Long asignacionId);

    /**
     * Actividad del dia del colaborador (RF-17 / mi-actividad).
     * Retorna los contactos registrados entre :inicio y :fin cuya asignacion
     * pertenece al colaborador (personal_id = :usuarioId).
     * Orden descendente para mostrar los mas recientes primero.
     */
    @Query("SELECT c FROM Contacto c " +
           "JOIN FETCH c.asignacion a " +
           "JOIN FETCH a.prospecto p " +
           "WHERE a.usuario.id = :usuarioId " +
           "AND c.fechaContacto >= :inicio " +
           "AND c.fechaContacto <= :fin " +
           "ORDER BY c.fechaContacto DESC")
    List<Contacto> findActividadDelDia(
            @Param("usuarioId") Long usuarioId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin);

    /**
     * Historial completo de contactos de un prospecto a traves de todos sus ciclos
     * (RF-04 / wizard). Incluye contactos de SBS (verificacionSbs != null) y de llamada.
     * Orden descendente por fecha para mostrar el mas reciente primero.
     */
    @Query("SELECT c FROM Contacto c " +
           "JOIN FETCH c.asignacion a " +
           "WHERE a.prospecto.prospectoID = :prospectoId " +
           "ORDER BY c.fechaContacto DESC")
    List<Contacto> findHistorialByProspectoId(@Param("prospectoId") Long prospectoId);

    // =========================================================================
    // Reportes RF-18 (dashboard / metricas del dueno)
    // =========================================================================

    /** Cuenta contactos de un colaborador en un rango de fechas (atenciones del periodo). */
    @Query("SELECT COUNT(c) FROM Contacto c " +
           "WHERE c.asignacion.usuario.id = :usuarioId " +
           "AND c.fechaContacto >= :inicio AND c.fechaContacto <= :fin")
    long countAtencionesColaborador(
            @Param("usuarioId") Long usuarioId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin);

    /** Cuenta contactos en un rango de fechas (atenciones globales del periodo). */
    @Query("SELECT COUNT(c) FROM Contacto c " +
           "WHERE c.fechaContacto >= :inicio AND c.fechaContacto <= :fin")
    long countAtencionesPeriodo(
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin);

    /**
     * Contactabilidad real del periodo para un colaborador.
     * Devuelve List con un Object[] de [countTitular, countConResultado].
     * Se usa List para evitar el comportamiento de Spring Data de "desenvolver"
     * un array de un solo elemento cuando hay exactamente una fila de resultado.
     */
    @Query("SELECT " +
           "  SUM(CASE WHEN c.quienContesto = com.pe.swcotoschero.prospectos.Entity.enums.QuienContesto.TITULAR THEN 1L ELSE 0L END), " +
           "  SUM(CASE WHEN c.estadoResultado IS NOT NULL THEN 1L ELSE 0L END) " +
           "FROM Contacto c " +
           "WHERE c.asignacion.usuario.id = :usuarioId " +
           "AND c.fechaContacto >= :inicio AND c.fechaContacto <= :fin")
    List<Object[]> contactabilidadColaborador(
            @Param("usuarioId") Long usuarioId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin);

    /**
     * Contactabilidad real global del periodo.
     * Devuelve List con un Object[] de [countTitular, countConResultado].
     */
    @Query("SELECT " +
           "  SUM(CASE WHEN c.quienContesto = com.pe.swcotoschero.prospectos.Entity.enums.QuienContesto.TITULAR THEN 1L ELSE 0L END), " +
           "  SUM(CASE WHEN c.estadoResultado IS NOT NULL THEN 1L ELSE 0L END) " +
           "FROM Contacto c " +
           "WHERE c.fechaContacto >= :inicio AND c.fechaContacto <= :fin")
    List<Object[]> contactabilidadGlobal(
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin);

    /** Ultima actividad (max fechaContacto) de un colaborador. */
    @Query("SELECT MAX(c.fechaContacto) FROM Contacto c WHERE c.asignacion.usuario.id = :usuarioId")
    LocalDateTime ultimaActividadColaborador(@Param("usuarioId") Long usuarioId);

    /**
     * Colaboradores distintos que registraron al menos un contacto hoy.
     */
    @Query("SELECT COUNT(DISTINCT c.asignacion.usuario.id) FROM Contacto c " +
           "WHERE c.fechaContacto >= :inicio AND c.fechaContacto <= :fin")
    long countColaboradoresActivosHoy(
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin);

    /**
     * Cuenta contactos de un colaborador en un rango con quienContesto=TITULAR.
     */
    @Query("SELECT COUNT(c) FROM Contacto c " +
           "WHERE c.asignacion.usuario.id = :usuarioId " +
           "AND c.fechaContacto >= :inicio AND c.fechaContacto <= :fin " +
           "AND c.quienContesto = com.pe.swcotoschero.prospectos.Entity.enums.QuienContesto.TITULAR")
    long countTitularColaborador(
            @Param("usuarioId") Long usuarioId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin);

    /**
     * Cuenta contactos con estadoResultado != null de un colaborador en un rango.
     */
    @Query("SELECT COUNT(c) FROM Contacto c " +
           "WHERE c.asignacion.usuario.id = :usuarioId " +
           "AND c.fechaContacto >= :inicio AND c.fechaContacto <= :fin " +
           "AND c.estadoResultado IS NOT NULL")
    long countConResultadoColaborador(
            @Param("usuarioId") Long usuarioId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin);

    /** Total contactos de una asignacion (para drill-down). */
    @Query("SELECT COUNT(c) FROM Contacto c WHERE c.asignacion.asignacionID = :asignacionId")
    long countPorAsignacion(@Param("asignacionId") Long asignacionId);
}
