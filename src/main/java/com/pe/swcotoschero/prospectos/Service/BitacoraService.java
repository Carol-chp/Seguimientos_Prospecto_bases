package com.pe.swcotoschero.prospectos.Service;

import com.pe.swcotoschero.prospectos.Entity.Asignacion;
import com.pe.swcotoschero.prospectos.Entity.Contacto;
import com.pe.swcotoschero.prospectos.Entity.Prospecto;
import com.pe.swcotoschero.prospectos.Entity.Usuario;
import com.pe.swcotoschero.prospectos.Entity.enums.QuienContesto;
import com.pe.swcotoschero.prospectos.Entity.enums.ResultadoAtencion;
import com.pe.swcotoschero.prospectos.Repository.BitacoraRepository;
import com.pe.swcotoschero.prospectos.dto.reporte.BitacoraFilaDTO;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * RF-20 / §5h — bitácora global de atenciones para el dueño.
 *
 * - Búsqueda paginada con filtros opcionales (fecha/rango, colaborador,
 *   campaña, base, resultado, quién contestó).
 * - Export a Excel del resultado filtrado (sin paginar, cap razonable).
 * - Celular enmascarado (mismo criterio que el resto del sistema).
 */
@Service
public class BitacoraService {

    private static final int MAX_SIZE = 200;
    private static final int EXPORT_CAP = 10_000;

    private final BitacoraRepository bitacoraRepository;

    public BitacoraService(BitacoraRepository bitacoraRepository) {
        this.bitacoraRepository = bitacoraRepository;
    }

    /** Búsqueda paginada. Devuelve {total, totalPaginas, pagina, tamano, resultados}. */
    @Transactional(readOnly = true)
    public Map<String, Object> buscar(String desdeStr, String hastaStr, Long colaboradorId,
                                      String campania, Long baseId, String resultadoStr,
                                      String quienContestoStr, int pagina, int tamano) {
        Filtros f = parseFiltros(desdeStr, hastaStr, campania, resultadoStr, quienContestoStr);
        int page = Math.max(pagina, 1);
        int size = Math.min(Math.max(tamano, 1), MAX_SIZE);

        Page<Contacto> pg = bitacoraRepository.buscar(
                f.desde, f.hasta, colaboradorId, f.campania, baseId,
                f.resultado, f.quienContesto, PageRequest.of(page - 1, size));

        List<BitacoraFilaDTO> filas = pg.getContent().stream().map(this::aFila).toList();
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("total", pg.getTotalElements());
        r.put("totalPaginas", pg.getTotalPages());
        r.put("pagina", page);
        r.put("tamano", size);
        r.put("resultados", filas);
        return r;
    }

    /** Export Excel del resultado filtrado (cap {@value #EXPORT_CAP}). */
    @Transactional(readOnly = true)
    public byte[] exportar(String desdeStr, String hastaStr, Long colaboradorId,
                           String campania, Long baseId, String resultadoStr,
                           String quienContestoStr) throws IOException {
        Filtros f = parseFiltros(desdeStr, hastaStr, campania, resultadoStr, quienContestoStr);
        List<Contacto> rows = bitacoraRepository.buscar(
                f.desde, f.hasta, colaboradorId, f.campania, baseId,
                f.resultado, f.quienContesto, PageRequest.of(0, EXPORT_CAP)).getContent();

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Bitacora");
            CellStyle headerStyle = crearEstiloEncabezado(wb);
            String[] headers = {
                "Fecha", "Colaborador", "Prospecto", "Celular", "Convenio", "Base",
                "Resultado", "Submotivo", "Quien contestó", "SBS", "Duración (s)", "Comentario"
            };
            Row hr = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell c = hr.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(headerStyle);
            }
            int idx = 1;
            for (Contacto c : rows) {
                BitacoraFilaDTO d = aFila(c);
                Row row = sheet.createRow(idx++);
                row.createCell(0).setCellValue(nvl(d.getFecha()));
                row.createCell(1).setCellValue(nvl(d.getColaborador()));
                row.createCell(2).setCellValue(nvl(d.getProspecto()));
                row.createCell(3).setCellValue(nvl(d.getCelular()));
                row.createCell(4).setCellValue(nvl(d.getCampania()));
                row.createCell(5).setCellValue(nvl(d.getBase()));
                row.createCell(6).setCellValue(nvl(d.getEstadoResultado()));
                row.createCell(7).setCellValue(nvl(d.getSubmotivoNoContesto()));
                row.createCell(8).setCellValue(nvl(d.getQuienContesto()));
                row.createCell(9).setCellValue(nvl(d.getVerificacionSbs()));
                row.createCell(10).setCellValue(d.getDuracionGestion() != null ? d.getDuracionGestion() : 0);
                row.createCell(11).setCellValue(nvl(d.getComentario()));
            }
            for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        }
    }

    // ------------------------------------------------------------------ helpers

    private record Filtros(LocalDateTime desde, LocalDateTime hasta, String campania,
                           ResultadoAtencion resultado, QuienContesto quienContesto) {}

    private Filtros parseFiltros(String desdeStr, String hastaStr, String campania,
                                 String resultadoStr, String quienContestoStr) {
        LocalDateTime desde = parseInicioDia(desdeStr);
        LocalDateTime hasta = parseFinDia(hastaStr);
        if (desde != null && hasta != null && hasta.isBefore(desde)) {
            throw new IllegalArgumentException("La fecha 'hasta' no puede ser anterior a 'desde'.");
        }
        return new Filtros(
                desde, hasta,
                (campania == null || campania.isBlank()) ? null : campania.trim(),
                parseEnum(ResultadoAtencion.class, resultadoStr),
                parseEnum(QuienContesto.class, quienContestoStr));
    }

    private LocalDateTime parseInicioDia(String s) {
        LocalDate d = parseFecha(s);
        return d == null ? null : d.atStartOfDay();
    }

    private LocalDateTime parseFinDia(String s) {
        LocalDate d = parseFecha(s);
        return d == null ? null : d.atTime(LocalTime.MAX);
    }

    private LocalDate parseFecha(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return LocalDate.parse(s.trim());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Fecha inválida: '" + s + "' (formato esperado AAAA-MM-DD).");
        }
    }

    private <E extends Enum<E>> E parseEnum(Class<E> clazz, String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Enum.valueOf(clazz, value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Valor '" + value + "' no es válido para " + clazz.getSimpleName());
        }
    }

    private BitacoraFilaDTO aFila(Contacto c) {
        Asignacion a = c.getAsignacion();
        Prospecto p = a != null ? a.getProspecto() : null;
        Usuario u = a != null ? a.getUsuario() : null;
        return BitacoraFilaDTO.builder()
                .contactoId(c.getContactoID())
                .asignacionId(a != null ? a.getAsignacionID() : null)
                .prospectoId(p != null ? p.getProspectoID() : null)
                .fecha(c.getFechaContacto() != null ? c.getFechaContacto().toString() : null)
                .colaborador(u != null ? nombre(u.getNombre(), u.getApellidos()) : null)
                .prospecto(p != null ? nombre(p.getNombre(), p.getApellido()) : null)
                .celular(p != null ? mask(p.getCelular()) : null)
                .campania(p != null && p.getCampania() != null ? p.getCampania().getNombre() : null)
                .base(p != null && p.getCargaMasiva() != null ? p.getCargaMasiva().getNombrearchivo() : null)
                .estadoResultado(c.getEstadoResultado() != null ? c.getEstadoResultado().name() : null)
                .submotivoNoContesto(c.getSubmotivoNoContesto() != null ? c.getSubmotivoNoContesto().name() : null)
                .quienContesto(c.getQuienContesto() != null ? c.getQuienContesto().name() : null)
                .verificacionSbs(c.getVerificacionSbs() != null ? c.getVerificacionSbs().name() : null)
                .duracionGestion(c.getDuracionGestion())
                .comentario(c.getComentario())
                .build();
    }

    private static String nombre(String n, String a) {
        return ((n != null ? n.trim() : "") + " " + (a != null ? a.trim() : "")).trim();
    }

    private static String mask(String v) {
        if (v == null || v.isBlank()) return "";
        String t = v.trim();
        return t.length() <= 3 ? "***" : "*".repeat(t.length() - 3) + t.substring(t.length() - 3);
    }

    private static String nvl(String v) {
        return v != null ? v : "";
    }

    private CellStyle crearEstiloEncabezado(XSSFWorkbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }
}
