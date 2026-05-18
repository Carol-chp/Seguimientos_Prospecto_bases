package com.pe.swcotoschero.prospectos.Service;

import com.pe.swcotoschero.prospectos.Entity.AperturaEvento;
import com.pe.swcotoschero.prospectos.Entity.Asignacion;
import com.pe.swcotoschero.prospectos.Entity.ConfiguracionDueno;
import com.pe.swcotoschero.prospectos.Entity.Contacto;
import com.pe.swcotoschero.prospectos.Entity.Prospecto;
import com.pe.swcotoschero.prospectos.Entity.Usuario;
import com.pe.swcotoschero.prospectos.Entity.enums.EstadoGestion;
import com.pe.swcotoschero.prospectos.Entity.enums.QuienContesto;
import com.pe.swcotoschero.prospectos.Entity.enums.ResultadoAtencion;
import com.pe.swcotoschero.prospectos.Entity.enums.SubmotivoNoContesto;
import com.pe.swcotoschero.prospectos.Entity.enums.VerificacionSbs;
import com.pe.swcotoschero.prospectos.Repository.AperturaEventoRepository;
import com.pe.swcotoschero.prospectos.Repository.AsignacionRepository;
import com.pe.swcotoschero.prospectos.Repository.ConfiguracionDuenoRepository;
import com.pe.swcotoschero.prospectos.Repository.ContactoRepository;
import com.pe.swcotoschero.prospectos.Repository.ProspectoRepository;
import com.pe.swcotoschero.prospectos.dto.AperturaResponseDTO;
import com.pe.swcotoschero.prospectos.dto.ContactoRegistroDTO;
import com.pe.swcotoschero.prospectos.dto.HistorialContactoDTO;
import com.pe.swcotoschero.prospectos.dto.VerificacionSbsRequestDTO;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.pe.swcotoschero.prospectos.Entity.enums.EstadoGestion.DESCARTADO;
import static com.pe.swcotoschero.prospectos.Entity.enums.EstadoGestion.GANADO;

/**
 * Logica de negocio del Wizard de atencion (RF-04/13/14/15/16).
 *
 * Flujo nominal:
 *  1. POST /api/contactos/apertura              → crea AperturaEvento, cronometro arranca
 *  2. POST /api/contactos/verificacion-sbs      → Paso 0 bloqueante: APTO continua, OBSERVADO detiene
 *  3. POST /api/contactos                       → registra la atencion, cierra cronometro
 *
 * Reglas de transicion de estado (registrarContacto):
 *  NO_CONTESTO  → intentos++; si > max → DESCARTADO/ILOCALIZABLE; si no → EN_SEGUIMIENTO + proximaLlamada
 *  AGENDADO / VOLVER_LLAMAR → EN_SEGUIMIENTO + fechaAgenda (obligatorio en DTO)
 *  INTERESADO   → EN_GESTION
 *  DERIVADO     → DERIVADO + derivadoPor + fechaDerivacion
 *  NO_VOLVER_LLAMAR / DATOS_INVALIDOS → DESCARTADO
 */
@Service
public class ContactoService {

    private static final DateTimeFormatter ISO_DT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm[:ss]");

    private final ContactoRepository contactoRepository;
    private final AsignacionRepository asignacionRepository;
    private final ProspectoRepository prospectoRepository;
    private final AperturaEventoRepository aperturaEventoRepository;
    private final ConfiguracionDuenoRepository configuracionDuenoRepository;
    private final EmailService emailService;
    private final AsistenciaService asistenciaService;

    public ContactoService(ContactoRepository contactoRepository,
                           AsignacionRepository asignacionRepository,
                           ProspectoRepository prospectoRepository,
                           AperturaEventoRepository aperturaEventoRepository,
                           ConfiguracionDuenoRepository configuracionDuenoRepository,
                           EmailService emailService,
                           AsistenciaService asistenciaService) {
        this.contactoRepository = contactoRepository;
        this.asignacionRepository = asignacionRepository;
        this.prospectoRepository = prospectoRepository;
        this.aperturaEventoRepository = aperturaEventoRepository;
        this.configuracionDuenoRepository = configuracionDuenoRepository;
        this.emailService = emailService;
        this.asistenciaService = asistenciaService;
    }

    // =========================================================================
    // RF-14 — Apertura del modal
    // =========================================================================

    /**
     * Crea un AperturaEvento para el ciclo activo del prospecto.
     * inicio = now; huboRegistro = false (se corrige al guardar la atencion).
     *
     * @param callerUsuarioId ID del usuario autenticado (para verificar ownership).
     */
    @Transactional
    public AperturaResponseDTO abrirModal(Long prospectoId, Long callerUsuarioId) {
        Asignacion asignacion = resolverCicloActivo(prospectoId);
        verificarOwnership(asignacion, callerUsuarioId);

        AperturaEvento apertura = new AperturaEvento();
        apertura.setAsignacion(asignacion);
        apertura.setInicio(LocalDateTime.now());
        apertura.setHuboRegistro(false);

        AperturaEvento guardada = aperturaEventoRepository.save(apertura);
        return new AperturaResponseDTO(guardada.getId(), guardada.getInicio().toString());
    }

    /**
     * Cierra el modal sin guardar resultado (cancelacion por el usuario).
     * Idempotente: si ya tiene fin, no modifica nada.
     *
     * @param callerUsuarioId ID del usuario autenticado (para verificar ownership).
     */
    @Transactional
    public void cerrarModalSinRegistro(Long aperturaId, Long callerUsuarioId) {
        AperturaEvento apertura = aperturaEventoRepository.findById(aperturaId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "AperturaEvento no encontrado: " + aperturaId));

        verificarOwnership(apertura.getAsignacion(), callerUsuarioId);

        if (apertura.getFin() != null) {
            return; // idempotente
        }

        apertura.setFin(LocalDateTime.now());
        apertura.setHuboRegistro(false);
        aperturaEventoRepository.save(apertura);
    }

    // =========================================================================
    // RF-15 — Verificacion SBS (Paso 0, bloqueante)
    // =========================================================================

    /**
     * Registra el resultado de la consulta SBS sobre el ciclo activo.
     *
     * APTO     → { continuar: true }
     * OBSERVADO → { continuar: false, estado: "EN_SEGUIMIENTO", fechaReevaluacionSbs: "YYYY-MM-DD" }
     *
     * @param callerUsuarioId ID del usuario autenticado (para verificar ownership).
     */
    @Transactional
    public Map<String, Object> verificarSbs(VerificacionSbsRequestDTO dto, Long callerUsuarioId) {
        VerificacionSbs resultado = parsearVerificacionSbs(dto.getResultado());

        Asignacion asignacion = resolverCicloActivo(dto.getProspectoId());
        verificarOwnership(asignacion, callerUsuarioId);
        LocalDateTime ahora = LocalDateTime.now();

        asignacion.setVerificacionSbs(resultado);
        asignacion.setFechaConsultaSbs(ahora);

        Map<String, Object> respuesta = new LinkedHashMap<>();

        if (resultado == VerificacionSbs.APTO) {
            asignacionRepository.save(asignacion);
            respuesta.put("continuar", true);

        } else {
            // OBSERVADO: calcular fecha de reevaluacion
            LocalDate fechaReeval = resolverFechaReevaluacion(dto.getFechaReevaluacion());

            asignacion.setFechaReevaluacionSbs(fechaReeval);
            asignacion.setEstado(EstadoGestion.EN_SEGUIMIENTO);

            // Agenda a las 09:00 del dia de reevaluacion → cola D2 lo reactiva
            LocalDateTime agendaReeval = fechaReeval.atTime(LocalTime.of(9, 0));
            asignacion.setFechaAgenda(agendaReeval);

            // Crear contacto de SBS (historico del evento; estadoResultado null = no es llamada)
            Contacto contactoSbs = new Contacto();
            contactoSbs.setAsignacion(asignacion);
            contactoSbs.setFechaContacto(ahora);
            contactoSbs.setVerificacionSbs(VerificacionSbs.OBSERVADO);
            contactoSbs.setFechaConsultaSbs(ahora);
            contactoSbs.setComentario(dto.getComentario());

            asignacionRepository.save(asignacion);
            contactoRepository.save(contactoSbs);

            respuesta.put("continuar", false);
            respuesta.put("estado", EstadoGestion.EN_SEGUIMIENTO.name());
            respuesta.put("fechaReevaluacionSbs", fechaReeval.toString());
        }

        return respuesta;
    }

    // =========================================================================
    // RF-04 / RF-13 / RF-16 — Registrar atencion
    // =========================================================================

    /**
     * Registra una atencion completa sobre el ciclo activo del prospecto.
     *
     * @return { ok: true, estado: "...", proximaLlamada: "ISO|null" }
     */
    @Transactional
    public Map<String, Object> registrarContacto(ContactoRegistroDTO dto,
                                                  Usuario usuarioAutenticado) {
        Asignacion asignacion = resolverCicloActivo(dto.getProspectoId());
        verificarOwnership(asignacion, usuarioAutenticado.getId());

        // Bloqueo SBS
        if (asignacion.getVerificacionSbs() != VerificacionSbs.APTO) {
            throw new IllegalArgumentException(
                    "Debe verificar SBS (APTO) antes de registrar la llamada.");
        }

        // Resolver resultado (nuevo campo primero; alias legacy como fallback)
        String rawResultado = dto.getResultado() != null
                ? dto.getResultado()
                : dto.getEstadoResultado();

        ResultadoAtencion resultado = parsearResultado(rawResultado);

        // Validar submotivo para NO_CONTESTO
        SubmotivoNoContesto submotivo = null;
        if (resultado == ResultadoAtencion.NO_CONTESTO) {
            submotivo = parsearSubmotivo(dto.getSubmotivoNoContesto());
        }

        // Parsear quienContesto (opcional)
        QuienContesto quienContesto = null;
        if (dto.getQuienContesto() != null && !dto.getQuienContesto().isBlank()) {
            quienContesto = parsearQuienContesto(dto.getQuienContesto());
        }

        LocalDateTime ahora = LocalDateTime.now();
        ConfiguracionDueno config = obtenerConfiguracion();

        // Construir Contacto
        Contacto contacto = new Contacto();
        contacto.setAsignacion(asignacion);
        contacto.setFechaContacto(ahora);
        contacto.setEstadoResultado(resultado);
        contacto.setSubmotivoNoContesto(submotivo);
        contacto.setQuienContesto(quienContesto);
        contacto.setComentario(dto.getComentario());
        // Compatibilidad deprecada
        contacto.setContestoLlamada(resultado != ResultadoAtencion.NO_CONTESTO);
        contacto.setInteresado(resultado == ResultadoAtencion.INTERESADO);

        // Cronometro / apertura (RF-13/14)
        Integer duracion = resolverDuracion(dto, ahora);
        if (duracion != null) {
            contacto.setDuracionGestion(duracion);
            asignacion.setDuracionGestion(duracion);
        }

        // Transicion de estado
        LocalDateTime proximaLlamada = aplicarTransicion(
                resultado, dto, asignacion, usuarioAutenticado, ahora, config, contacto);

        // Actualizar campos de llamada en la asignacion
        if (quienContesto != null) {
            asignacion.setQuienContesto(quienContesto);
        }
        if (submotivo != null) {
            asignacion.setSubmotivoNoContesto(submotivo.name());
        }

        contactoRepository.save(contacto);
        asignacionRepository.save(asignacion);

        // Actualizar estadoInteresado en Prospecto (campo legacy)
        Prospecto prospecto = asignacion.getProspecto();
        prospecto.setEstadoInteresado(resultado == ResultadoAtencion.INTERESADO);
        prospectoRepository.save(prospecto);

        // RF-06a/06b: notificación al dueño (async, best-effort, no bloquea
        // el registro; respeta toggles y app.mail.enabled internamente).
        // Fire AFTER commit so the async thread always reads committed data.
        final Long contactoId = contacto.getContactoID();
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            emailService.notificarAtencionAsync(contactoId);
                        }
                    });
        } else {
            // No active transaction (e.g. called from a test without @Transactional) —
            // fall back to direct call so behaviour is preserved.
            emailService.notificarAtencionAsync(contactoId);
        }

        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("ok", true);
        respuesta.put("estado", asignacion.getEstado().name());
        respuesta.put("proximaLlamada",
                proximaLlamada != null ? proximaLlamada.toString() : null);
        return respuesta;
    }

    // =========================================================================
    // Historial
    // =========================================================================

    /**
     * Devuelve el historial de contactos del prospecto.
     *
     * @param callerUsuarioId ID del usuario autenticado.
     * @param callerIsAdmin   true si el caller tiene rol ADMINISTRADOR.
     */
    @Transactional(readOnly = true)
    public List<HistorialContactoDTO> obtenerHistorial(Long prospectoId,
                                                        Long callerUsuarioId,
                                                        boolean callerIsAdmin) {
        if (!callerIsAdmin) {
            // Verify that the prospecto has an active cycle owned by the caller.
            // We use the same cycle-resolution logic (finds non-terminal cycle); if the
            // prospecto has no active cycle at all, the caller has no business viewing
            // its historial either.
            Asignacion asignacion = asignacionRepository
                    .findFirstByProspecto_ProspectoIDAndEstadoNotInOrderByFechaAsignacionDesc(
                            prospectoId,
                            List.of(com.pe.swcotoschero.prospectos.Entity.enums.EstadoGestion.GANADO,
                                    DESCARTADO))
                    .orElseThrow(() -> new AccessDeniedException(
                            "No tiene acceso al historial de este prospecto."));
            verificarOwnership(asignacion, callerUsuarioId);
        }
        return contactoRepository.findHistorialByProspectoId(prospectoId)
                .stream()
                .map(c -> new HistorialContactoDTO(
                        c.getFechaContacto() != null ? c.getFechaContacto().toString() : null,
                        c.getEstadoResultado() != null ? c.getEstadoResultado().name() : null,
                        c.getSubmotivoNoContesto() != null ? c.getSubmotivoNoContesto().name() : null,
                        c.getQuienContesto() != null ? c.getQuienContesto().name() : null,
                        c.getVerificacionSbs() != null ? c.getVerificacionSbs().name() : null,
                        c.getComentario(),
                        c.getDuracionGestion()))
                .collect(Collectors.toList());
    }

    // =========================================================================
    // Helpers privados
    // =========================================================================

    private Asignacion resolverCicloActivo(Long prospectoId) {
        return asignacionRepository
                .findFirstByProspecto_ProspectoIDAndEstadoNotInOrderByFechaAsignacionDesc(
                        prospectoId, List.of(GANADO, DESCARTADO))
                .orElseThrow(() -> new IllegalArgumentException(
                        "El prospecto con ID " + prospectoId
                        + " no tiene un ciclo activo."));
    }

    /**
     * Verifica que el ciclo activo pertenece al usuario autenticado.
     * Lanza AccessDeniedException si no coincide (GlobalExceptionHandler → 403).
     */
    private void verificarOwnership(Asignacion asignacion, Long callerUsuarioId) {
        if (asignacion.getUsuario() == null
                || !asignacion.getUsuario().getId().equals(callerUsuarioId)) {
            throw new AccessDeniedException(
                    "No tiene permisos para operar sobre este prospecto.");
        }
    }

    private ConfiguracionDueno obtenerConfiguracion() {
        return configuracionDuenoRepository.findTopByOrderByIdAsc()
                .orElseGet(ConfiguracionDueno::new);
    }

    private VerificacionSbs parsearVerificacionSbs(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException(
                    "El campo 'resultado' es obligatorio. Valores: APTO, OBSERVADO.");
        }
        try {
            return VerificacionSbs.valueOf(raw.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Resultado SBS no valido: '" + raw + "'. Valores: APTO, OBSERVADO.");
        }
    }

    private LocalDate resolverFechaReevaluacion(String fechaStr) {
        if (fechaStr != null && !fechaStr.isBlank()) {
            try {
                return LocalDate.parse(fechaStr.trim());
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException(
                        "Formato de fechaReevaluacion invalido: '" + fechaStr
                        + "'. Use YYYY-MM-DD.");
            }
        }
        ConfiguracionDueno config = obtenerConfiguracion();
        return LocalDate.now().plusDays(config.getPlazoReevaluacionSbsDias());
    }

    private ResultadoAtencion parsearResultado(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException(
                    "El campo 'resultado' es obligatorio. Valores: "
                    + Arrays.stream(ResultadoAtencion.values())
                            .map(Enum::name)
                            .collect(Collectors.joining(", ")));
        }
        try {
            return ResultadoAtencion.valueOf(raw.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Resultado de atencion no valido: '" + raw + "'. Valores: "
                    + Arrays.stream(ResultadoAtencion.values())
                            .map(Enum::name)
                            .collect(Collectors.joining(", ")));
        }
    }

    private SubmotivoNoContesto parsearSubmotivo(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException(
                    "submotivoNoContesto es obligatorio cuando resultado = NO_CONTESTO. "
                    + "Valores: NO_CONTESTA, BUZON, OCUPADO, APAGADO.");
        }
        try {
            return SubmotivoNoContesto.valueOf(raw.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "submotivoNoContesto no valido: '" + raw
                    + "'. Valores: NO_CONTESTA, BUZON, OCUPADO, APAGADO.");
        }
    }

    private QuienContesto parsearQuienContesto(String raw) {
        try {
            return QuienContesto.valueOf(raw.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "quienContesto no valido: '" + raw
                    + "'. Valores: TITULAR, TERCERO, EQUIVOCADO.");
        }
    }

    private LocalDateTime parseFechaAgenda(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException(
                    "fechaAgenda es obligatorio para resultado AGENDADO o VOLVER_LLAMAR. "
                    + "Formato: yyyy-MM-dd'T'HH:mm o yyyy-MM-dd'T'HH:mm:ss");
        }
        try {
            return LocalDateTime.parse(raw.trim(), ISO_DT);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(
                    "Formato de fechaAgenda invalido: '" + raw
                    + "'. Use yyyy-MM-dd'T'HH:mm o yyyy-MM-dd'T'HH:mm:ss");
        }
    }

    /**
     * Valida una fecha de agenda para AGENDADO / VOLVER_LLAMAR (3.1 — robustez):
     *  - debe ser futura (no se puede agendar/recontactar en el pasado);
     *  - debe caer en un día laborable (no domingo ni feriado de la empresa),
     *    reusando la misma regla del calendario laboral (RF-22) para no agendar
     *    en un día en que nadie trabajará el caso.
     */
    private void validarAgenda(LocalDateTime agenda, LocalDateTime ahora) {
        if (!agenda.isAfter(ahora)) {
            throw new IllegalArgumentException(
                    "La fecha de agenda debe ser futura (posterior a la fecha y hora actual).");
        }
        if (!asistenciaService.esDiaLaborable(agenda.toLocalDate())) {
            throw new IllegalArgumentException(
                    "La fecha de agenda cae en un día no laborable (domingo o feriado). "
                    + "Elija un día laborable.");
        }
    }

    /**
     * Resuelve la duracion de la gestion:
     *  1. Si hay aperturaId: cierra el evento; usa duracionGestionSegundos si viene, si no calcula.
     *  2. Si no hay aperturaId: usa duracionGestionSegundos si viene.
     *  3. Devuelve null si no hay ninguna fuente de duracion.
     */
    private Integer resolverDuracion(ContactoRegistroDTO dto, LocalDateTime ahora) {
        if (dto.getAperturaId() != null) {
            AperturaEvento apertura = aperturaEventoRepository
                    .findById(dto.getAperturaId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "AperturaEvento no encontrado: " + dto.getAperturaId()));

            apertura.setFin(ahora);
            apertura.setHuboRegistro(true);
            aperturaEventoRepository.save(apertura);

            if (dto.getDuracionGestionSegundos() != null) {
                return dto.getDuracionGestionSegundos();
            }
            return (int) Duration.between(apertura.getInicio(), ahora).getSeconds();
        }

        return dto.getDuracionGestionSegundos();
    }

    /**
     * Aplica la transicion de estado sobre la asignacion segun el resultado.
     * Devuelve la proximaLlamada calculada (null si no aplica).
     * El parametro contacto se modifica si el resultado se sobrescribe a ILOCALIZABLE.
     */
    private LocalDateTime aplicarTransicion(ResultadoAtencion resultado,
                                             ContactoRegistroDTO dto,
                                             Asignacion asignacion,
                                             Usuario usuarioAutenticado,
                                             LocalDateTime ahora,
                                             ConfiguracionDueno config,
                                             Contacto contacto) {
        switch (resultado) {

            case NO_CONTESTO: {
                int intentos = (asignacion.getIntentosFallidos() != null
                        ? asignacion.getIntentosFallidos() : 0) + 1;
                asignacion.setIntentosFallidos(intentos);

                if (intentos > config.getMaxIntentosNoContesto()) {
                    asignacion.setEstado(DESCARTADO);
                    asignacion.setEstadoResultado(ResultadoAtencion.ILOCALIZABLE);
                    asignacion.setFechaAgenda(null);
                    asignacion.setProximaLlamada(null);
                    // El contacto refleja el resultado efectivo
                    contacto.setEstadoResultado(ResultadoAtencion.ILOCALIZABLE);
                    return null;
                } else {
                    LocalDateTime proxima = calcularProximaLlamada(
                            ahora, intentos, config.getReglaReintentoNoContesto());
                    asignacion.setEstado(EstadoGestion.EN_SEGUIMIENTO);
                    asignacion.setEstadoResultado(ResultadoAtencion.NO_CONTESTO);
                    asignacion.setProximaLlamada(proxima);
                    asignacion.setFechaAgenda(proxima);
                    return proxima;
                }
            }

            case AGENDADO:
            case VOLVER_LLAMAR: {
                LocalDateTime agenda = parseFechaAgenda(dto.getFechaAgenda());
                validarAgenda(agenda, ahora);
                asignacion.setEstado(EstadoGestion.EN_SEGUIMIENTO);
                asignacion.setEstadoResultado(resultado);
                asignacion.setFechaAgenda(agenda);
                asignacion.setProximaLlamada(agenda);
                return agenda;
            }

            case INTERESADO: {
                asignacion.setEstado(EstadoGestion.EN_GESTION);
                asignacion.setEstadoResultado(resultado);
                asignacion.setFechaAgenda(null);
                return null;
            }

            case DERIVADO: {
                asignacion.setEstado(EstadoGestion.DERIVADO);
                asignacion.setEstadoResultado(resultado);
                asignacion.setDerivadoPor(usuarioAutenticado);
                asignacion.setFechaDerivacion(ahora);
                asignacion.setFechaAgenda(null);
                return null;
            }

            case NO_VOLVER_LLAMAR:
            case DATOS_INVALIDOS: {
                asignacion.setEstado(DESCARTADO);
                asignacion.setEstadoResultado(resultado);
                asignacion.setFechaAgenda(null);
                return null;
            }

            case ILOCALIZABLE: {
                throw new IllegalArgumentException(
                        "ILOCALIZABLE es un estado interno calculado automaticamente. "
                        + "Use NO_CONTESTO para incrementar el contador de intentos.");
            }

            default:
                throw new IllegalArgumentException("Resultado no manejado: " + resultado);
        }
    }

    /**
     * Calcula la proxima llamada segun la regla de reintentos escalonada.
     * regla: CSV de offsets "+Nh", ej: "+3h,+24h,+48h,+72h,+120h".
     * indice = intentos-1; si excede la lista, usa el ultimo elemento.
     */
    private LocalDateTime calcularProximaLlamada(LocalDateTime base,
                                                  int intentos,
                                                  String regla) {
        if (regla == null || regla.isBlank()) {
            return base.plusHours(3);
        }
        String[] partes = regla.split(",");
        int indice = Math.min(intentos - 1, partes.length - 1);
        String offset = partes[indice].trim();

        if (offset.startsWith("+") && offset.endsWith("h")) {
            try {
                long horas = Long.parseLong(offset.substring(1, offset.length() - 1));
                return base.plusHours(horas);
            } catch (NumberFormatException ignored) {
                // fallthrough a default
            }
        }
        return base.plusHours(3);
    }

    // =========================================================================
    // Metodos de compatibilidad mantenidos (usados por tests existentes o
    // endpoints de otros controladores que llaman a este service)
    // =========================================================================

    public Optional<Contacto> obtenerPorId(Long id) {
        return contactoRepository.findById(id);
    }

    public Contacto guardar(Contacto contacto) {
        return contactoRepository.save(contacto);
    }

    public void eliminar(Long id) {
        contactoRepository.deleteById(id);
    }
}
