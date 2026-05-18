package com.pe.swcotoschero.prospectos.Service;

import com.pe.swcotoschero.prospectos.Entity.Asignacion;
import com.pe.swcotoschero.prospectos.Entity.Contacto;
import com.pe.swcotoschero.prospectos.Entity.Prospecto;
import com.pe.swcotoschero.prospectos.Entity.enums.EstadoGestion;
import com.pe.swcotoschero.prospectos.Entity.enums.FiltroColaborador;
import com.pe.swcotoschero.prospectos.Entity.enums.ResultadoAtencion;
import com.pe.swcotoschero.prospectos.Repository.AsignacionRepository;
import com.pe.swcotoschero.prospectos.Repository.ContactoRepository;
import com.pe.swcotoschero.prospectos.dto.MiActividadDTO;
import com.pe.swcotoschero.prospectos.dto.MiProspectoDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Lógica de negocio de la cola del colaborador (RF-17).
 * Encapsula el routing de filtros, el enmascarado de datos sensibles,
 * y el cálculo de las banderas vencido/futuro.
 *
 * Zona horaria: America/Lima (UTC-5, sin horario de verano).
 */
@Service
@Transactional(readOnly = true)
public class ColaboradorColaService {

    private static final ZoneId ZONA_LIMA = ZoneId.of("America/Lima");

    /** Valores permitidos de filtro (para el mensaje 400). */
    public static final List<String> FILTROS_PERMITIDOS =
            Arrays.stream(FiltroColaborador.values())
                  .map(Enum::name)
                  .collect(Collectors.toList());

    private final AsignacionRepository asignacionRepository;
    private final ContactoRepository contactoRepository;

    public ColaboradorColaService(AsignacionRepository asignacionRepository,
                                   ContactoRepository contactoRepository) {
        this.asignacionRepository = asignacionRepository;
        this.contactoRepository = contactoRepository;
    }

    // =========================================================================
    // Cola del colaborador
    // =========================================================================

    /**
     * Devuelve la pagina de asignaciones segun el filtro solicitado.
     *
     * @param usuarioId     ID del colaborador autenticado
     * @param filtroStr     Nombre del filtro (se valida contra FiltroColaborador)
     * @param busqueda      Texto libre (null o blank = sin filtro)
     * @param pagina        Pagina 1-based
     * @param tamanioPagina Registros por pagina
     * @throws IllegalArgumentException si el filtro no es valido (GlobalExceptionHandler -> 400)
     */
    public Map<String, Object> obtenerCola(Long usuarioId,
                                            String filtroStr,
                                            String busqueda,
                                            int pagina,
                                            int tamanioPagina) {

        FiltroColaborador filtro = parsearFiltro(filtroStr);
        String like = construirLike(busqueda);
        PageRequest pageRequest = PageRequest.of(Math.max(pagina - 1, 0), tamanioPagina);

        LocalDateTime ahora = LocalDateTime.now(ZONA_LIMA);
        LocalDateTime inicioDia = ahora.toLocalDate().atStartOfDay();
        LocalDateTime finDia = ahora.toLocalDate().atTime(LocalTime.MAX);

        Page<Asignacion> page = ejecutarFiltro(filtro, usuarioId, like, ahora, inicioDia, finDia, pageRequest);

        List<MiProspectoDTO> dtos = page.getContent().stream()
                .map(a -> mapearAsignacion(a, ahora, finDia))
                .collect(Collectors.toList());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("filtro", filtro.name());
        response.put("resultados", dtos);
        response.put("pagina", page.getNumber() + 1);
        response.put("tamanioPagina", page.getSize());
        response.put("total", page.getTotalElements());
        response.put("totalPaginas", page.getTotalPages());
        return response;
    }

    // =========================================================================
    // Actividad del dia
    // =========================================================================

    /**
     * Actividad del colaborador hoy: lista de contactos + resumen por resultado.
     *
     * @param usuarioId ID del colaborador autenticado
     */
    public MiActividadDTO obtenerActividadHoy(Long usuarioId) {
        LocalDateTime ahora = LocalDateTime.now(ZONA_LIMA);
        LocalDateTime inicio = ahora.toLocalDate().atStartOfDay();
        LocalDateTime fin = ahora.toLocalDate().atTime(LocalTime.MAX);

        List<Contacto> contactos = contactoRepository.findActividadDelDia(usuarioId, inicio, fin);

        Map<String, Long> resumen = contactos.stream()
                .filter(c -> c.getEstadoResultado() != null)
                .collect(Collectors.groupingBy(
                        c -> c.getEstadoResultado().name(),
                        Collectors.counting()));

        List<MiActividadDTO.ItemActividadDTO> items = contactos.stream()
                .map(c -> {
                    Prospecto p = c.getAsignacion().getProspecto();
                    return MiActividadDTO.ItemActividadDTO.builder()
                            .contactoId(c.getContactoID())
                            .asignacionId(c.getAsignacion().getAsignacionID())
                            .prospectoId(p.getProspectoID())
                            .nombreProspecto(nombreCompleto(p.getNombre(), p.getApellido()))
                            .celular(enmascararSensible(p.getCelular()))
                            .fechaContacto(c.getFechaContacto())
                            .estadoResultado(c.getEstadoResultado() != null
                                    ? c.getEstadoResultado().name() : null)
                            .comentario(c.getComentario())
                            .duracionGestion(c.getDuracionGestion())
                            .build();
                })
                .collect(Collectors.toList());

        return MiActividadDTO.builder()
                .generadoEn(ahora)
                .totalGestiones(contactos.size())
                .resumenPorResultado(resumen)
                .gestiones(items)
                .build();
    }

    // =========================================================================
    // Privados
    // =========================================================================

    private FiltroColaborador parsearFiltro(String filtroStr) {
        String valor = (filtroStr == null || filtroStr.isBlank()) ? "MI_COLA_HOY" : filtroStr.trim().toUpperCase();
        try {
            return FiltroColaborador.valueOf(valor);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Filtro '" + filtroStr + "' no valido. Valores permitidos: " + FILTROS_PERMITIDOS);
        }
    }

    /**
     * Convierte el texto de busqueda en un patron LIKE para JPQL.
     * Sin busqueda -> '%' (match todo).
     */
    private String construirLike(String busqueda) {
        if (busqueda == null || busqueda.isBlank()) {
            return "%";
        }
        // Escapa los caracteres especiales de LIKE antes de envolver
        String escaped = busqueda.trim()
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + escaped + "%";
    }

    private Page<Asignacion> ejecutarFiltro(FiltroColaborador filtro,
                                             Long usuarioId,
                                             String like,
                                             LocalDateTime ahora,
                                             LocalDateTime inicioDia,
                                             LocalDateTime finDia,
                                             PageRequest pageRequest) {
        return switch (filtro) {
            case MI_COLA_HOY -> asignacionRepository.findFiltroMiColaHoy(
                    usuarioId, like, ahora, finDia, pageRequest);
            case SIN_GESTIONAR -> asignacionRepository.findFiltroSinGestionar(
                    usuarioId, like, pageRequest);
            case AGENDADOS_HOY -> asignacionRepository.findFiltroAgendadosHoy(
                    usuarioId, like, inicioDia, finDia, pageRequest);
            case POR_REINTENTAR -> asignacionRepository.findFiltroPorReintentar(
                    usuarioId, like, ahora, pageRequest);
            case PROGRAMADOS -> asignacionRepository.findFiltroProgramados(
                    usuarioId, like, pageRequest);
            case OBSERVADO_SBS -> asignacionRepository.findFiltroObservadoSbs(
                    usuarioId, like, pageRequest);
            case DERIVADOS -> asignacionRepository.findFiltroDerivos(
                    usuarioId, like, pageRequest);
            case INTERESADOS -> asignacionRepository.findFiltroInteresados(
                    usuarioId, like, pageRequest);
            case MIS_VENTAS -> asignacionRepository.findFiltroMisVentas(
                    usuarioId, like, pageRequest);
            case DESCARTADOS -> asignacionRepository.findFiltroDescartados(
                    usuarioId, like, pageRequest);
            case TODOS -> asignacionRepository.findFiltroTodos(
                    usuarioId, like, pageRequest);
        };
    }

    /**
     * Mapea una Asignacion a MiProspectoDTO.
     * Recupera el ultimo contacto y el total de contactos como consultas independientes
     * (ya existe el indice idx_contacto_asignacion — no es N+1 costoso en produccion normal).
     * Si en el futuro el volumen lo justifica, se puede proyectar directamente en la query.
     */
    private MiProspectoDTO mapearAsignacion(Asignacion a,
                                             LocalDateTime ahora,
                                             LocalDateTime finDia) {
        Prospecto p = a.getProspecto();

        long totalContactos = contactoRepository.countByAsignacion_AsignacionID(a.getAsignacionID());
        var ultimoContactoOpt = contactoRepository
                .findTopByAsignacion_AsignacionIDOrderByFechaContactoDesc(a.getAsignacionID());

        boolean esEnSeguimiento = EstadoGestion.EN_SEGUIMIENTO == a.getEstado();
        boolean vencido = esEnSeguimiento
                && a.getFechaAgenda() != null
                && a.getFechaAgenda().isBefore(ahora);
        boolean futuro = esEnSeguimiento
                && a.getFechaAgenda() != null
                && a.getFechaAgenda().isAfter(finDia);

        return MiProspectoDTO.builder()
                .asignacionId(a.getAsignacionID())
                .prospectoId(p.getProspectoID())
                .nombre(p.getNombre())
                .apellido(p.getApellido())
                .celular(enmascararSensible(p.getCelular()))
                .celularMasked(true)
                .documentoIdentidad(enmascararSensible(p.getDocumentoIdentidad()))
                .campania(p.getCampania() != null ? p.getCampania().getNombre() : null)
                .estado(a.getEstado() != null ? a.getEstado().name() : null)
                .estadoResultado(a.getEstadoResultado() != null ? a.getEstadoResultado().name() : null)
                .fechaAgenda(a.getFechaAgenda())
                .verificacionSbs(a.getVerificacionSbs() != null ? a.getVerificacionSbs().name() : null)
                .fechaReevaluacionSbs(a.getFechaReevaluacionSbs())
                .proximaLlamada(a.getProximaLlamada())
                .intentosFallidos(a.getIntentosFallidos() != null ? a.getIntentosFallidos() : 0)
                .ultimoContacto(ultimoContactoOpt.map(Contacto::getFechaContacto).orElse(null))
                .totalContactos((int) totalContactos)
                .nroPrestamosConcretados(
                        p.getNroPrestamosConcretados() != null ? p.getNroPrestamosConcretados() : 0)
                .vencido(vencido)
                .futuro(futuro)
                .build();
    }

    /**
     * Enmascara un valor sensible dejando visibles solo los ultimos 3 caracteres.
     * "987654321" -> "*****321"
     * Null o cadena muy corta (< 3) -> devuelve "***".
     */
    static String enmascararSensible(String valor) {
        if (valor == null || valor.isBlank()) {
            return "***";
        }
        if (valor.length() <= 3) {
            return "***";
        }
        int visibles = 3;
        int ocultos = valor.length() - visibles;
        return "*".repeat(ocultos) + valor.substring(ocultos);
    }

    private static String nombreCompleto(String nombre, String apellido) {
        String n = nombre != null ? nombre.trim() : "";
        String a = apellido != null ? apellido.trim() : "";
        return (n + " " + a).trim();
    }
}
