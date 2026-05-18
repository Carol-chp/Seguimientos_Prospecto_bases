package com.pe.swcotoschero.prospectos.Service;

import com.pe.swcotoschero.prospectos.Entity.Asignacion;
import com.pe.swcotoschero.prospectos.Entity.Contacto;
import com.pe.swcotoschero.prospectos.Entity.Prospecto;
import com.pe.swcotoschero.prospectos.Entity.Usuario;
import com.pe.swcotoschero.prospectos.Entity.enums.EstadoGestion;
import com.pe.swcotoschero.prospectos.Entity.enums.ResultadoAtencion;
import com.pe.swcotoschero.prospectos.Repository.AsignacionRepository;
import com.pe.swcotoschero.prospectos.Repository.ContactoRepository;
import com.pe.swcotoschero.prospectos.Repository.ProspectoRepository;
import com.pe.swcotoschero.prospectos.dto.PorCerrarItemDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Cierre de venta por el DUEÑO (5c.bis / D4).
 *
 * El colaborador NO cierra: deriva (estado DERIVADO). El dueño, sobre esos casos:
 *  - registra VENTA → GANADO (inmutable; fechaElegibilidad obligatoria — D7)
 *  - "no cerró": REINTENTAR → EN_SEGUIMIENTO(fecha) | DESCARTAR → DESCARTADO
 *
 * La venta queda atribuida al colaborador que derivó (asignacion.derivadoPor — D4);
 * aquí NO se reatribuye nada.
 */
@Service
public class CierreService {

    private static final DateTimeFormatter ISO_DT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm[:ss]");

    private final AsignacionRepository asignacionRepository;
    private final ProspectoRepository prospectoRepository;
    private final ContactoRepository contactoRepository;

    public CierreService(AsignacionRepository asignacionRepository,
                         ProspectoRepository prospectoRepository,
                         ContactoRepository contactoRepository) {
        this.asignacionRepository = asignacionRepository;
        this.prospectoRepository = prospectoRepository;
        this.contactoRepository = contactoRepository;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> listarPorCerrar(int pagina, int tamanioPagina) {
        Page<Asignacion> page = asignacionRepository.findPorCerrar(
                PageRequest.of(pagina > 0 ? pagina - 1 : 0, tamanioPagina));

        var items = page.getContent().stream().map(a -> {
            Prospecto p = a.getProspecto();
            Usuario der = a.getDerivadoPor();
            return PorCerrarItemDTO.builder()
                    .asignacionId(a.getAsignacionID())
                    .prospectoId(p.getProspectoID())
                    .nombre(p.getNombre())
                    .apellido(p.getApellido())
                    .celular(mask(p.getCelular()))
                    .celularMasked(true)
                    .documentoIdentidad(mask(p.getDocumentoIdentidad()))
                    .campania(p.getCampania() != null ? p.getCampania().getNombre() : null)
                    .derivadoPorId(der != null ? der.getId() : null)
                    .derivadoPorNombre(der != null
                            ? (der.getNombre() + " " + der.getApellidos()) : null)
                    .fechaDerivacion(a.getFechaDerivacion())
                    .nroPrestamosConcretados(p.getNroPrestamosConcretados() != null
                            ? p.getNroPrestamosConcretados() : 0)
                    .build();
        }).toList();

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("resultados", items);
        r.put("pagina", page.getNumber() + 1);
        r.put("tamanioPagina", page.getSize());
        r.put("total", page.getTotalElements());
        r.put("totalPaginas", page.getTotalPages());
        return r;
    }

    /** Registra la VENTA de un caso DERIVADO → GANADO (inmutable). */
    @Transactional
    public Map<String, Object> registrarVenta(Long asignacionId, String fechaElegibilidadStr,
                                               String comentario, Usuario admin) {
        Asignacion a = cargarDerivado(asignacionId);

        if (fechaElegibilidadStr == null || fechaElegibilidadStr.isBlank()) {
            throw new IllegalArgumentException(
                    "La fecha de elegibilidad para un nuevo préstamo es obligatoria al registrar la venta.");
        }
        LocalDate fechaElegibilidad;
        try {
            fechaElegibilidad = LocalDate.parse(fechaElegibilidadStr.trim());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(
                    "Fecha de elegibilidad inválida (use YYYY-MM-DD): " + fechaElegibilidadStr);
        }
        if (fechaElegibilidad.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException(
                    "La fecha de elegibilidad debe ser futura.");
        }

        LocalDateTime ahora = LocalDateTime.now();
        a.setEstado(EstadoGestion.GANADO);
        a.setCerradoPor(admin);
        a.setFechaCierre(ahora);
        a.setFechaElegibilidad(fechaElegibilidad);
        a.setFechaAgenda(null);
        asignacionRepository.save(a);

        Prospecto p = a.getProspecto();
        p.setNroPrestamosConcretados(
                (p.getNroPrestamosConcretados() != null ? p.getNroPrestamosConcretados() : 0) + 1);
        prospectoRepository.save(p);

        registrarTraza(a, "VENTA registrada por el dueño. " + safe(comentario), ahora);

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("ok", true);
        r.put("estado", EstadoGestion.GANADO.name());
        r.put("prospectoId", p.getProspectoID());
        r.put("derivadoPorId", a.getDerivadoPor() != null ? a.getDerivadoPor().getId() : null);
        r.put("fechaElegibilidad", fechaElegibilidad.toString());
        r.put("nroPrestamosConcretados", p.getNroPrestamosConcretados());
        return r;
    }

    /** El dueño no cerró: REINTENTAR (EN_SEGUIMIENTO con fecha) o DESCARTAR. */
    @Transactional
    public Map<String, Object> noCerro(Long asignacionId, String accion,
                                       String fechaStr, String comentario, Usuario admin) {
        Asignacion a = cargarDerivado(asignacionId);
        String acc = accion == null ? "" : accion.trim().toUpperCase();
        LocalDateTime ahora = LocalDateTime.now();

        if ("REINTENTAR".equals(acc)) {
            if (fechaStr == null || fechaStr.isBlank()) {
                throw new IllegalArgumentException(
                        "La fecha de reintento es obligatoria para REINTENTAR.");
            }
            LocalDateTime fecha;
            try {
                fecha = LocalDateTime.parse(fechaStr.trim(), ISO_DT);
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException(
                        "Fecha de reintento inválida (use yyyy-MM-ddTHH:mm): " + fechaStr);
            }
            a.setEstado(EstadoGestion.EN_SEGUIMIENTO);
            a.setEstadoResultado(ResultadoAtencion.VOLVER_LLAMAR);
            a.setFechaAgenda(fecha);
        } else if ("DESCARTAR".equals(acc)) {
            a.setEstado(EstadoGestion.DESCARTADO);
            a.setEstadoResultado(ResultadoAtencion.NO_VOLVER_LLAMAR);
            a.setFechaAgenda(null);
        } else {
            throw new IllegalArgumentException(
                    "Acción no válida: " + accion + ". Use REINTENTAR o DESCARTAR.");
        }
        a.setCerradoPor(admin);
        a.setFechaCierre(ahora);
        asignacionRepository.save(a);

        registrarTraza(a, "El dueño no cerró (" + acc + "). " + safe(comentario), ahora);

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("ok", true);
        r.put("estado", a.getEstado().name());
        return r;
    }

    // ---------------------------------------------------------------------

    private Asignacion cargarDerivado(Long asignacionId) {
        Asignacion a = asignacionRepository.findById(asignacionId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Asignación no encontrada: " + asignacionId));
        if (a.getEstado() != EstadoGestion.DERIVADO) {
            throw new IllegalArgumentException(
                    "Solo se puede cerrar un caso en estado DERIVADO. Estado actual: "
                    + a.getEstado() + (a.getEstado() == EstadoGestion.GANADO
                        ? " (la venta ya fue registrada; GANADO es inmutable)." : "."));
        }
        return a;
    }

    private void registrarTraza(Asignacion a, String comentario, LocalDateTime ahora) {
        Contacto c = new Contacto();
        c.setAsignacion(a);
        c.setFechaContacto(ahora);
        c.setComentario(comentario);
        contactoRepository.save(c);
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    /** Enmascara dejando los últimos 3 caracteres visibles. */
    private static String mask(String v) {
        if (v == null || v.isBlank()) return "";
        String t = v.trim();
        if (t.length() <= 3) return "***";
        return "*".repeat(t.length() - 3) + t.substring(t.length() - 3);
    }
}
