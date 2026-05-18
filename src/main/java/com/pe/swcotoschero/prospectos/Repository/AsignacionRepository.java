package com.pe.swcotoschero.prospectos.Repository;

import com.pe.swcotoschero.prospectos.Entity.Asignacion;
import com.pe.swcotoschero.prospectos.Entity.Prospecto;
import com.pe.swcotoschero.prospectos.Entity.enums.EstadoGestion;
import com.pe.swcotoschero.prospectos.Entity.enums.ResultadoAtencion;
import com.pe.swcotoschero.prospectos.Entity.enums.VerificacionSbs;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface AsignacionRepository extends JpaRepository<Asignacion, Long> {

    /**
     * Ciclo ACTIVO de un prospecto = la asignacion en un estado NO terminal.
     * Un prospecto puede tener varios ciclos en el tiempo (GANADO/DESCARTADO
     * historicos, inmutables) + a lo sumo UNO activo. Esta consulta excluye
     * los terminales y, por seguridad ante datos inconsistentes, toma el mas
     * reciente por fecha de asignacion.
     *
     * @param estadosTerminales normalmente {GANADO, DESCARTADO}
     */
    Optional<Asignacion> findFirstByProspecto_ProspectoIDAndEstadoNotInOrderByFechaAsignacionDesc(
            Long prospectoId, Collection<EstadoGestion> estadosTerminales);

    /** Encuentra todas las asignaciones para una lista de prospectos. */
    List<Asignacion> findByProspectoIn(List<Prospecto> prospectos);

    /** Cuenta asignaciones por estado (enum canonico). */
    long countByEstado(EstadoGestion estado);

    /** Verifica si existen asignaciones para un usuario como colaborador. */
    boolean existsByUsuario_Id(Long usuarioId);

    /** Verifica si existen asignaciones para un usuario como administrador. */
    boolean existsByAdministrador_Id(Long administradorId);

    /**
     * Cuenta asignaciones agrupadas por usuario para una carga masiva.
     * Retorna [usuarioId, nombre, apellidos, count].
     */
    @Query("SELECT a.usuario.id, a.usuario.nombre, a.usuario.apellidos, COUNT(a) " +
           "FROM Asignacion a WHERE a.prospecto.cargaMasiva.id = :cargaMasivaId " +
           "GROUP BY a.usuario.id, a.usuario.nombre, a.usuario.apellidos")
    List<Object[]> countAssignmentsByUserForCargaMasiva(@Param("cargaMasivaId") Long cargaMasivaId);

    /** Cuenta total de asignaciones para una carga masiva. */
    @Query("SELECT COUNT(a) FROM Asignacion a WHERE a.prospecto.cargaMasiva.id = :cargaMasivaId")
    Long countByCargaMasivaId(@Param("cargaMasivaId") Long cargaMasivaId);

    /**
     * Encuentra asignaciones de un usuario con filtros opcionales (metodo legado — mantenido
     * para compatibilidad hacia atras. Los nuevos consumidores usan los metodos findFiltroXxx).
     * Acepta enums directamente (null = sin filtro).
     */
    @Query("SELECT a FROM Asignacion a " +
           "JOIN FETCH a.prospecto p " +
           "LEFT JOIN FETCH p.campania " +
           "WHERE a.usuario.id = :usuarioId " +
           "AND (:estado IS NULL OR a.estado = :estado) " +
           "AND (:estadoResultado IS NULL OR a.estadoResultado = :estadoResultado) " +
           "ORDER BY CASE " +
           "  WHEN a.estado = com.pe.swcotoschero.prospectos.Entity.enums.EstadoGestion.SIN_GESTIONAR THEN 0 " +
           "  WHEN a.estado = com.pe.swcotoschero.prospectos.Entity.enums.EstadoGestion.EN_GESTION THEN 1 " +
           "  WHEN a.estado = com.pe.swcotoschero.prospectos.Entity.enums.EstadoGestion.EN_SEGUIMIENTO THEN 2 " +
           "  ELSE 3 END, " +
           "a.fechaAsignacion DESC")
    Page<Asignacion> findByUsuarioWithFilters(
            @Param("usuarioId") Long usuarioId,
            @Param("estado") EstadoGestion estado,
            @Param("estadoResultado") ResultadoAtencion estadoResultado,
            Pageable pageable);

    /** Cuenta asignaciones por estado para un usuario (enum canonico). */
    long countByUsuario_IdAndEstado(Long usuarioId, EstadoGestion estado);

    /** Cuenta asignaciones por estado resultado para un usuario (enum canonico). */
    long countByUsuario_IdAndEstadoResultado(Long usuarioId, ResultadoAtencion estadoResultado);

    /**
     * "Por cerrar" del dueño (5c.bis): ciclos en estado DERIVADO, con el prospecto,
     * su campaña y el colaborador que derivó (derivadoPor) ya cargados (evita N+1).
     * Orden: derivación más antigua primero (el que espera hace más tiempo).
     */
    @Query(value = "SELECT a FROM Asignacion a " +
           "JOIN FETCH a.prospecto p " +
           "LEFT JOIN FETCH p.campania " +
           "LEFT JOIN FETCH a.derivadoPor " +
           "WHERE a.estado = com.pe.swcotoschero.prospectos.Entity.enums.EstadoGestion.DERIVADO " +
           "ORDER BY a.fechaDerivacion ASC NULLS LAST, a.fechaAsignacion ASC",
           countQuery = "SELECT COUNT(a) FROM Asignacion a " +
           "WHERE a.estado = com.pe.swcotoschero.prospectos.Entity.enums.EstadoGestion.DERIVADO")
    Page<Asignacion> findPorCerrar(Pageable pageable);

    /**
     * Casos ACTIVOS de un colaborador (reasignables): SIN_GESTIONAR, EN_GESTION,
     * EN_SEGUIMIENTO. Excluye DERIVADO/GANADO/DESCARTADO (no se reasignan — 5j).
     */
    @Query("SELECT a FROM Asignacion a JOIN FETCH a.prospecto p LEFT JOIN FETCH p.campania " +
           "WHERE a.usuario.id = :usuarioId AND a.estado IN (" +
           "  com.pe.swcotoschero.prospectos.Entity.enums.EstadoGestion.SIN_GESTIONAR," +
           "  com.pe.swcotoschero.prospectos.Entity.enums.EstadoGestion.EN_GESTION," +
           "  com.pe.swcotoschero.prospectos.Entity.enums.EstadoGestion.EN_SEGUIMIENTO)")
    List<Asignacion> findActivasByUsuario(@Param("usuarioId") Long usuarioId);

    /**
     * "En riesgo" (5j): casos de colaboradores ausentes hoy que son SIN_GESTIONAR
     * o EN_SEGUIMIENTO vencido/de hoy (fechaAgenda <= fin de hoy). El estado NO se
     * cambia (D2); es un bucket para que el dueño reasigne.
     */
    @Query("SELECT a FROM Asignacion a JOIN FETCH a.prospecto p " +
           "LEFT JOIN FETCH p.campania JOIN FETCH a.usuario u " +
           "WHERE u.id IN :ausentes AND (" +
           "  a.estado = com.pe.swcotoschero.prospectos.Entity.enums.EstadoGestion.SIN_GESTIONAR " +
           "  OR (a.estado = com.pe.swcotoschero.prospectos.Entity.enums.EstadoGestion.EN_SEGUIMIENTO " +
           "      AND a.fechaAgenda IS NOT NULL AND a.fechaAgenda <= :finHoy))")
    List<Asignacion> findEnRiesgo(@Param("ausentes") java.util.Collection<Long> ausentes,
                                  @Param("finHoy") LocalDateTime finHoy);

    /**
     * Job D7: ciclos GANADO cuya fecha de elegibilidad ya venció y aún no se
     * han reactivado (fechaElegibilidad != null). Se carga prospecto/usuario/admin
     * para clonar el ciclo nuevo sin N+1. El registro GANADO NO se muta (solo se
     * limpia su fechaElegibilidad para no reprocesarlo).
     */
    @Query("SELECT a FROM Asignacion a " +
           "JOIN FETCH a.prospecto " +
           "JOIN FETCH a.usuario " +
           "JOIN FETCH a.administrador " +
           "WHERE a.estado = com.pe.swcotoschero.prospectos.Entity.enums.EstadoGestion.GANADO " +
           "AND a.fechaElegibilidad IS NOT NULL AND a.fechaElegibilidad <= :hoy")
    List<Asignacion> findGanadosReelegibles(@Param("hoy") java.time.LocalDate hoy);

    /**
     * Job (red de seguridad): EN_SEGUIMIENTO/NO_CONTESTO que superaron el máximo
     * de intentos pero quedaron sin descartar (el flujo normal ya lo hace inline).
     */
    @Query("SELECT a FROM Asignacion a " +
           "WHERE a.estado = com.pe.swcotoschero.prospectos.Entity.enums.EstadoGestion.EN_SEGUIMIENTO " +
           "AND a.estadoResultado = com.pe.swcotoschero.prospectos.Entity.enums.ResultadoAtencion.NO_CONTESTO " +
           "AND a.intentosFallidos > :max")
    List<Asignacion> findIlocalizablesPendientes(@Param("max") int max);

    // =========================================================================
    // FILTROS RF-17 — cola del colaborador (11 filtros exactos)
    //
    // Convencion de orden en cada query:
    //  1. FETCH del prospecto y campania en la misma pasada (evita N+1).
    //  2. Parametros: usuarioId (Long), mas los especificos del filtro.
    //  3. El orden final es responsabilidad del servicio cuando necesita
    //     una logica mixta (MI_COLA_HOY); los demas filtros usan ORDER BY simple.
    //
    // IMPORTANTE: los literales de enum en JPQL deben ser totalmente calificados.
    // Patron ya establecido en findByUsuarioWithFilters (lineas de arriba).
    // =========================================================================

    /**
     * MI_COLA_HOY — todo lo accionable: SIN_GESTIONAR, EN_GESTION, y EN_SEGUIMIENTO
     * con cualquier fechaAgenda (incluye futuros; el servicio calcula vencido/futuro).
     * Excluye DISPONIBLE, DERIVADO, GANADO, DESCARTADO.
     *
     * Orden:
     *   1. SIN_GESTIONAR (prioridad maxima — nunca tocados)
     *   2. EN_GESTION
     *   3. EN_SEGUIMIENTO con fechaAgenda <= :ahora (vencidos — deben atenderse ya)
     *   4. EN_SEGUIMIENTO con fechaAgenda dentro del dia (agendados hoy)
     *   5. EN_SEGUIMIENTO con fechaAgenda futura (atenuar en UI)
     *   6. EN_SEGUIMIENTO sin fechaAgenda (configuracion incompleta, al final)
     *
     * La busqueda por texto (:busqueda) aplica ILIKE sobre nombre, apellido,
     * celular y documentoIdentidad. Pasar '%' cuando no hay texto de busqueda.
     */
    @Query("SELECT a FROM Asignacion a " +
           "JOIN FETCH a.prospecto p " +
           "LEFT JOIN FETCH p.campania " +
           "WHERE a.usuario.id = :usuarioId " +
           "AND a.estado IN (" +
           "  com.pe.swcotoschero.prospectos.Entity.enums.EstadoGestion.SIN_GESTIONAR," +
           "  com.pe.swcotoschero.prospectos.Entity.enums.EstadoGestion.EN_GESTION," +
           "  com.pe.swcotoschero.prospectos.Entity.enums.EstadoGestion.EN_SEGUIMIENTO" +
           ") " +
           "AND (LOWER(p.nombre) LIKE LOWER(:busqueda) " +
           "  OR LOWER(p.apellido) LIKE LOWER(:busqueda) " +
           "  OR LOWER(p.celular) LIKE LOWER(:busqueda) " +
           "  OR LOWER(p.documentoIdentidad) LIKE LOWER(:busqueda)) " +
           "ORDER BY " +
           "CASE " +
           "  WHEN a.estado = com.pe.swcotoschero.prospectos.Entity.enums.EstadoGestion.SIN_GESTIONAR THEN 0 " +
           "  WHEN a.estado = com.pe.swcotoschero.prospectos.Entity.enums.EstadoGestion.EN_GESTION THEN 1 " +
           "  WHEN a.estado = com.pe.swcotoschero.prospectos.Entity.enums.EstadoGestion.EN_SEGUIMIENTO " +
           "       AND a.fechaAgenda IS NOT NULL AND a.fechaAgenda <= :ahora THEN 2 " +
           "  WHEN a.estado = com.pe.swcotoschero.prospectos.Entity.enums.EstadoGestion.EN_SEGUIMIENTO " +
           "       AND a.fechaAgenda IS NOT NULL AND a.fechaAgenda > :ahora AND a.fechaAgenda <= :finDia THEN 3 " +
           "  WHEN a.estado = com.pe.swcotoschero.prospectos.Entity.enums.EstadoGestion.EN_SEGUIMIENTO " +
           "       AND a.fechaAgenda IS NOT NULL AND a.fechaAgenda > :finDia THEN 4 " +
           "  ELSE 5 END, " +
           "a.fechaAgenda ASC NULLS LAST, " +
           "a.fechaAsignacion ASC")
    Page<Asignacion> findFiltroMiColaHoy(
            @Param("usuarioId") Long usuarioId,
            @Param("busqueda") String busqueda,
            @Param("ahora") LocalDateTime ahora,
            @Param("finDia") LocalDateTime finDia,
            Pageable pageable);

    /**
     * SIN_GESTIONAR — asignados pero nunca tocados.
     */
    @Query("SELECT a FROM Asignacion a " +
           "JOIN FETCH a.prospecto p " +
           "LEFT JOIN FETCH p.campania " +
           "WHERE a.usuario.id = :usuarioId " +
           "AND a.estado = com.pe.swcotoschero.prospectos.Entity.enums.EstadoGestion.SIN_GESTIONAR " +
           "AND (LOWER(p.nombre) LIKE LOWER(:busqueda) " +
           "  OR LOWER(p.apellido) LIKE LOWER(:busqueda) " +
           "  OR LOWER(p.celular) LIKE LOWER(:busqueda) " +
           "  OR LOWER(p.documentoIdentidad) LIKE LOWER(:busqueda)) " +
           "ORDER BY a.fechaAsignacion ASC")
    Page<Asignacion> findFiltroSinGestionar(
            @Param("usuarioId") Long usuarioId,
            @Param("busqueda") String busqueda,
            Pageable pageable);

    /**
     * AGENDADOS_HOY — EN_SEGUIMIENTO + AGENDADO + fechaAgenda dentro de hoy.
     */
    @Query("SELECT a FROM Asignacion a " +
           "JOIN FETCH a.prospecto p " +
           "LEFT JOIN FETCH p.campania " +
           "WHERE a.usuario.id = :usuarioId " +
           "AND a.estado = com.pe.swcotoschero.prospectos.Entity.enums.EstadoGestion.EN_SEGUIMIENTO " +
           "AND a.estadoResultado = com.pe.swcotoschero.prospectos.Entity.enums.ResultadoAtencion.AGENDADO " +
           "AND a.fechaAgenda >= :inicioDia " +
           "AND a.fechaAgenda <= :finDia " +
           "AND (LOWER(p.nombre) LIKE LOWER(:busqueda) " +
           "  OR LOWER(p.apellido) LIKE LOWER(:busqueda) " +
           "  OR LOWER(p.celular) LIKE LOWER(:busqueda) " +
           "  OR LOWER(p.documentoIdentidad) LIKE LOWER(:busqueda)) " +
           "ORDER BY a.fechaAgenda ASC")
    Page<Asignacion> findFiltroAgendadosHoy(
            @Param("usuarioId") Long usuarioId,
            @Param("busqueda") String busqueda,
            @Param("inicioDia") LocalDateTime inicioDia,
            @Param("finDia") LocalDateTime finDia,
            Pageable pageable);

    /**
     * POR_REINTENTAR — EN_SEGUIMIENTO + NO_CONTESTO + fechaAgenda <= ahora (vencidos).
     */
    @Query("SELECT a FROM Asignacion a " +
           "JOIN FETCH a.prospecto p " +
           "LEFT JOIN FETCH p.campania " +
           "WHERE a.usuario.id = :usuarioId " +
           "AND a.estado = com.pe.swcotoschero.prospectos.Entity.enums.EstadoGestion.EN_SEGUIMIENTO " +
           "AND a.estadoResultado = com.pe.swcotoschero.prospectos.Entity.enums.ResultadoAtencion.NO_CONTESTO " +
           "AND a.fechaAgenda <= :ahora " +
           "AND (LOWER(p.nombre) LIKE LOWER(:busqueda) " +
           "  OR LOWER(p.apellido) LIKE LOWER(:busqueda) " +
           "  OR LOWER(p.celular) LIKE LOWER(:busqueda) " +
           "  OR LOWER(p.documentoIdentidad) LIKE LOWER(:busqueda)) " +
           "ORDER BY a.fechaAgenda ASC")
    Page<Asignacion> findFiltroPorReintentar(
            @Param("usuarioId") Long usuarioId,
            @Param("busqueda") String busqueda,
            @Param("ahora") LocalDateTime ahora,
            Pageable pageable);

    /**
     * PROGRAMADOS — todos los EN_SEGUIMIENTO (incluye futuros).
     */
    @Query("SELECT a FROM Asignacion a " +
           "JOIN FETCH a.prospecto p " +
           "LEFT JOIN FETCH p.campania " +
           "WHERE a.usuario.id = :usuarioId " +
           "AND a.estado = com.pe.swcotoschero.prospectos.Entity.enums.EstadoGestion.EN_SEGUIMIENTO " +
           "AND (LOWER(p.nombre) LIKE LOWER(:busqueda) " +
           "  OR LOWER(p.apellido) LIKE LOWER(:busqueda) " +
           "  OR LOWER(p.celular) LIKE LOWER(:busqueda) " +
           "  OR LOWER(p.documentoIdentidad) LIKE LOWER(:busqueda)) " +
           "ORDER BY a.fechaAgenda ASC NULLS LAST")
    Page<Asignacion> findFiltroProgramados(
            @Param("usuarioId") Long usuarioId,
            @Param("busqueda") String busqueda,
            Pageable pageable);

    /**
     * OBSERVADO_SBS — EN_SEGUIMIENTO + verificacionSbs=OBSERVADO.
     */
    @Query("SELECT a FROM Asignacion a " +
           "JOIN FETCH a.prospecto p " +
           "LEFT JOIN FETCH p.campania " +
           "WHERE a.usuario.id = :usuarioId " +
           "AND a.estado = com.pe.swcotoschero.prospectos.Entity.enums.EstadoGestion.EN_SEGUIMIENTO " +
           "AND a.verificacionSbs = com.pe.swcotoschero.prospectos.Entity.enums.VerificacionSbs.OBSERVADO " +
           "AND (LOWER(p.nombre) LIKE LOWER(:busqueda) " +
           "  OR LOWER(p.apellido) LIKE LOWER(:busqueda) " +
           "  OR LOWER(p.celular) LIKE LOWER(:busqueda) " +
           "  OR LOWER(p.documentoIdentidad) LIKE LOWER(:busqueda)) " +
           "ORDER BY a.fechaReevaluacionSbs ASC NULLS LAST")
    Page<Asignacion> findFiltroObservadoSbs(
            @Param("usuarioId") Long usuarioId,
            @Param("busqueda") String busqueda,
            Pageable pageable);

    /**
     * DERIVADOS — solo lectura para el colaborador.
     */
    @Query("SELECT a FROM Asignacion a " +
           "JOIN FETCH a.prospecto p " +
           "LEFT JOIN FETCH p.campania " +
           "WHERE a.usuario.id = :usuarioId " +
           "AND a.estado = com.pe.swcotoschero.prospectos.Entity.enums.EstadoGestion.DERIVADO " +
           "AND (LOWER(p.nombre) LIKE LOWER(:busqueda) " +
           "  OR LOWER(p.apellido) LIKE LOWER(:busqueda) " +
           "  OR LOWER(p.celular) LIKE LOWER(:busqueda) " +
           "  OR LOWER(p.documentoIdentidad) LIKE LOWER(:busqueda)) " +
           "ORDER BY a.fechaDerivacion DESC NULLS LAST")
    Page<Asignacion> findFiltroDerivos(
            @Param("usuarioId") Long usuarioId,
            @Param("busqueda") String busqueda,
            Pageable pageable);

    /**
     * INTERESADOS — EN_GESTION + estadoResultado=INTERESADO.
     */
    @Query("SELECT a FROM Asignacion a " +
           "JOIN FETCH a.prospecto p " +
           "LEFT JOIN FETCH p.campania " +
           "WHERE a.usuario.id = :usuarioId " +
           "AND a.estado = com.pe.swcotoschero.prospectos.Entity.enums.EstadoGestion.EN_GESTION " +
           "AND a.estadoResultado = com.pe.swcotoschero.prospectos.Entity.enums.ResultadoAtencion.INTERESADO " +
           "AND (LOWER(p.nombre) LIKE LOWER(:busqueda) " +
           "  OR LOWER(p.apellido) LIKE LOWER(:busqueda) " +
           "  OR LOWER(p.celular) LIKE LOWER(:busqueda) " +
           "  OR LOWER(p.documentoIdentidad) LIKE LOWER(:busqueda)) " +
           "ORDER BY a.fechaAsignacion DESC")
    Page<Asignacion> findFiltroInteresados(
            @Param("usuarioId") Long usuarioId,
            @Param("busqueda") String busqueda,
            Pageable pageable);

    /**
     * MIS_VENTAS — estado=GANADO.
     */
    @Query("SELECT a FROM Asignacion a " +
           "JOIN FETCH a.prospecto p " +
           "LEFT JOIN FETCH p.campania " +
           "WHERE a.usuario.id = :usuarioId " +
           "AND a.estado = com.pe.swcotoschero.prospectos.Entity.enums.EstadoGestion.GANADO " +
           "AND (LOWER(p.nombre) LIKE LOWER(:busqueda) " +
           "  OR LOWER(p.apellido) LIKE LOWER(:busqueda) " +
           "  OR LOWER(p.celular) LIKE LOWER(:busqueda) " +
           "  OR LOWER(p.documentoIdentidad) LIKE LOWER(:busqueda)) " +
           "ORDER BY a.fechaCierre DESC NULLS LAST")
    Page<Asignacion> findFiltroMisVentas(
            @Param("usuarioId") Long usuarioId,
            @Param("busqueda") String busqueda,
            Pageable pageable);

    /**
     * DESCARTADOS — estado=DESCARTADO.
     */
    @Query("SELECT a FROM Asignacion a " +
           "JOIN FETCH a.prospecto p " +
           "LEFT JOIN FETCH p.campania " +
           "WHERE a.usuario.id = :usuarioId " +
           "AND a.estado = com.pe.swcotoschero.prospectos.Entity.enums.EstadoGestion.DESCARTADO " +
           "AND (LOWER(p.nombre) LIKE LOWER(:busqueda) " +
           "  OR LOWER(p.apellido) LIKE LOWER(:busqueda) " +
           "  OR LOWER(p.celular) LIKE LOWER(:busqueda) " +
           "  OR LOWER(p.documentoIdentidad) LIKE LOWER(:busqueda)) " +
           "ORDER BY a.fechaAsignacion DESC")
    Page<Asignacion> findFiltroDescartados(
            @Param("usuarioId") Long usuarioId,
            @Param("busqueda") String busqueda,
            Pageable pageable);

    /**
     * TODOS — todas las asignaciones del colaborador, cualquier estado.
     */
    @Query("SELECT a FROM Asignacion a " +
           "JOIN FETCH a.prospecto p " +
           "LEFT JOIN FETCH p.campania " +
           "WHERE a.usuario.id = :usuarioId " +
           "AND (LOWER(p.nombre) LIKE LOWER(:busqueda) " +
           "  OR LOWER(p.apellido) LIKE LOWER(:busqueda) " +
           "  OR LOWER(p.celular) LIKE LOWER(:busqueda) " +
           "  OR LOWER(p.documentoIdentidad) LIKE LOWER(:busqueda)) " +
           "ORDER BY " +
           "CASE " +
           "  WHEN a.estado = com.pe.swcotoschero.prospectos.Entity.enums.EstadoGestion.SIN_GESTIONAR THEN 0 " +
           "  WHEN a.estado = com.pe.swcotoschero.prospectos.Entity.enums.EstadoGestion.EN_GESTION THEN 1 " +
           "  WHEN a.estado = com.pe.swcotoschero.prospectos.Entity.enums.EstadoGestion.EN_SEGUIMIENTO THEN 2 " +
           "  WHEN a.estado = com.pe.swcotoschero.prospectos.Entity.enums.EstadoGestion.DERIVADO THEN 3 " +
           "  WHEN a.estado = com.pe.swcotoschero.prospectos.Entity.enums.EstadoGestion.GANADO THEN 4 " +
           "  WHEN a.estado = com.pe.swcotoschero.prospectos.Entity.enums.EstadoGestion.DESCARTADO THEN 5 " +
           "  ELSE 6 END, " +
           "a.fechaAsignacion DESC")
    Page<Asignacion> findFiltroTodos(
            @Param("usuarioId") Long usuarioId,
            @Param("busqueda") String busqueda,
            Pageable pageable);

    // =========================================================================
    // Reportes RF-18 — metricas del dueno (dashboard + embudo + bases)
    // =========================================================================

    /**
     * Ventas cerradas (GANADO) en un periodo, atribuidas al derivadoPor si existe.
     * Usado para dia/mes en el dashboard y en el ranking.
     * Retorna asignacionID para solo contar (o pode ampliarse a proyeccion).
     */
    @Query("SELECT COUNT(a) FROM Asignacion a " +
           "WHERE a.estado = com.pe.swcotoschero.prospectos.Entity.enums.EstadoGestion.GANADO " +
           "AND a.fechaCierre >= :inicio AND a.fechaCierre <= :fin")
    long countVentasCerradasPeriodo(
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin);

    /**
     * Ventas cerradas (GANADO) en un periodo atribuidas a un colaborador especifico
     * (por derivadoPor si presente, sino por usuario).
     */
    @Query("SELECT COUNT(a) FROM Asignacion a " +
           "WHERE a.estado = com.pe.swcotoschero.prospectos.Entity.enums.EstadoGestion.GANADO " +
           "AND a.fechaCierre >= :inicio AND a.fechaCierre <= :fin " +
           "AND (COALESCE(a.derivadoPor.id, a.usuario.id) = :usuarioId)")
    long countVentasCerradasColaboradorPeriodo(
            @Param("usuarioId") Long usuarioId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin);

    /**
     * Derivaciones en un periodo (fecha_derivacion en el rango).
     */
    @Query("SELECT COUNT(a) FROM Asignacion a " +
           "WHERE a.fechaDerivacion >= :inicio AND a.fechaDerivacion <= :fin")
    long countDerivadosPeriodo(
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin);

    /**
     * Derivaciones en un periodo atribuidas a un colaborador especifico.
     */
    @Query("SELECT COUNT(a) FROM Asignacion a " +
           "WHERE a.fechaDerivacion >= :inicio AND a.fechaDerivacion <= :fin " +
           "AND (COALESCE(a.derivadoPor.id, a.usuario.id) = :usuarioId)")
    long countDerivadosColaboradorPeriodo(
            @Param("usuarioId") Long usuarioId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin);

    /**
     * Citas para hoy: EN_SEGUIMIENTO + AGENDADO + fechaAgenda dentro de hoy.
     */
    @Query("SELECT COUNT(a) FROM Asignacion a " +
           "WHERE a.estado = com.pe.swcotoschero.prospectos.Entity.enums.EstadoGestion.EN_SEGUIMIENTO " +
           "AND a.estadoResultado = com.pe.swcotoschero.prospectos.Entity.enums.ResultadoAtencion.AGENDADO " +
           "AND a.fechaAgenda >= :inicioDia AND a.fechaAgenda <= :finDia")
    long countCitasHoy(
            @Param("inicioDia") LocalDateTime inicioDia,
            @Param("finDia") LocalDateTime finDia);

    // --- Embudo global (sin filtro de fecha) ---

    /** Total asignaciones (asignados). */
    @Query("SELECT COUNT(a) FROM Asignacion a")
    long countTotalAsignaciones();

    /** Gestionados: estado != SIN_GESTIONAR. */
    @Query("SELECT COUNT(a) FROM Asignacion a " +
           "WHERE a.estado != com.pe.swcotoschero.prospectos.Entity.enums.EstadoGestion.SIN_GESTIONAR")
    long countGestionados();

    /**
     * Asignaciones distintas que tuvieron al menos un contacto con quienContesto=TITULAR.
     */
    @Query("SELECT COUNT(DISTINCT c.asignacion.asignacionID) FROM Contacto c " +
           "WHERE c.quienContesto = com.pe.swcotoschero.prospectos.Entity.enums.QuienContesto.TITULAR")
    long countAsignacionesContactadasTitular();

    /** Interesados: estado EN_GESTION. */
    @Query("SELECT COUNT(a) FROM Asignacion a " +
           "WHERE a.estado = com.pe.swcotoschero.prospectos.Entity.enums.EstadoGestion.EN_GESTION")
    long countInteresados();

    /** Derivados global: estado DERIVADO. */
    @Query("SELECT COUNT(a) FROM Asignacion a " +
           "WHERE a.estado = com.pe.swcotoschero.prospectos.Entity.enums.EstadoGestion.DERIVADO")
    long countDerivadosGlobal();

    /** Ventas global: estado GANADO. */
    @Query("SELECT COUNT(a) FROM Asignacion a " +
           "WHERE a.estado = com.pe.swcotoschero.prospectos.Entity.enums.EstadoGestion.GANADO")
    long countVentasGlobal();

    /** Total asignaciones en estados != SIN_GESTIONAR (avance de bases). */
    @Query("SELECT COUNT(a) FROM Asignacion a " +
           "WHERE a.estado != com.pe.swcotoschero.prospectos.Entity.enums.EstadoGestion.SIN_GESTIONAR")
    long countAvanceBasesMgmt();

    /** Total de prospectos sin ninguna asignacion (disponibles sin asignar). */
    @Query("SELECT COUNT(p) FROM Prospecto p WHERE NOT EXISTS " +
           "(SELECT a FROM Asignacion a WHERE a.prospecto = p)")
    long countProspectosSinAsignacion();

    /**
     * Asignaciones de un colaborador paginadas con prospecto y campania cargados.
     * Orden: estado activos primero, luego por fecha de asignacion descendente.
     */
    @Query(value = "SELECT a FROM Asignacion a " +
           "JOIN FETCH a.prospecto p " +
           "LEFT JOIN FETCH p.campania " +
           "WHERE a.usuario.id = :usuarioId " +
           "ORDER BY a.fechaAsignacion DESC",
           countQuery = "SELECT COUNT(a) FROM Asignacion a WHERE a.usuario.id = :usuarioId")
    Page<Asignacion> findByUsuarioPaginado(
            @Param("usuarioId") Long usuarioId,
            Pageable pageable);

    /**
     * Asignaciones con filtros para exportacion (todos los campos necesarios).
     * Filtros opcionales: campaniaNombre y estado. Null = sin filtro.
     */
    @Query("SELECT a FROM Asignacion a " +
           "JOIN FETCH a.prospecto p " +
           "LEFT JOIN FETCH p.campania " +
           "LEFT JOIN FETCH a.usuario u " +
           "WHERE (:campaniaNombre IS NULL OR p.campania.nombre = :campaniaNombre) " +
           "AND (:estado IS NULL OR a.estado = :estado) " +
           "AND (:estadoResultado IS NULL OR a.estadoResultado = :estadoResultado) " +
           "ORDER BY a.fechaAsignacion DESC")
    List<Asignacion> findParaExportacion(
            @Param("campaniaNombre") String campaniaNombre,
            @Param("estado") EstadoGestion estado,
            @Param("estadoResultado") ResultadoAtencion estadoResultado);

    /**
     * Paged variant of findParaExportacion — used by the export service to enforce
     * a hard row cap (EXPORT_CAP = 10_000) so unbounded exports cannot exhaust memory.
     *
     * Note: Spring Data JPA does not allow JOIN FETCH together with a Pageable count
     * query on the same JPQL, so we use a separate countQuery.
     */
    @Query(value = "SELECT a FROM Asignacion a " +
           "JOIN FETCH a.prospecto p " +
           "LEFT JOIN FETCH p.campania " +
           "LEFT JOIN FETCH a.usuario u " +
           "WHERE (:campaniaNombre IS NULL OR p.campania.nombre = :campaniaNombre) " +
           "AND (:estado IS NULL OR a.estado = :estado) " +
           "AND (:estadoResultado IS NULL OR a.estadoResultado = :estadoResultado) " +
           "ORDER BY a.fechaAsignacion DESC",
           countQuery = "SELECT COUNT(a) FROM Asignacion a " +
           "JOIN a.prospecto p " +
           "WHERE (:campaniaNombre IS NULL OR p.campania.nombre = :campaniaNombre) " +
           "AND (:estado IS NULL OR a.estado = :estado) " +
           "AND (:estadoResultado IS NULL OR a.estadoResultado = :estadoResultado)")
    Page<Asignacion> findParaExportacionPaged(
            @Param("campaniaNombre") String campaniaNombre,
            @Param("estado") EstadoGestion estado,
            @Param("estadoResultado") ResultadoAtencion estadoResultado,
            Pageable pageable);

    /**
     * Prospectos de una carga masiva que tienen al menos una asignacion.
     * Para calcular asignados/sinAsignar por base.
     */
    @Query("SELECT COUNT(DISTINCT a.prospecto.prospectoID) FROM Asignacion a " +
           "WHERE a.prospecto.cargaMasiva.id = :cargaMasivaId")
    long countProspectosAsignadosPorCarga(@Param("cargaMasivaId") Long cargaMasivaId);
}
