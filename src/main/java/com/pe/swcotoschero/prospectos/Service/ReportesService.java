package com.pe.swcotoschero.prospectos.Service;

import com.pe.swcotoschero.prospectos.Entity.Asignacion;
import com.pe.swcotoschero.prospectos.Entity.CargaMasiva;
import com.pe.swcotoschero.prospectos.Entity.Usuario;
import com.pe.swcotoschero.prospectos.Entity.enums.EstadoGestion;
import com.pe.swcotoschero.prospectos.Entity.enums.FiltroColaborador;
import com.pe.swcotoschero.prospectos.Entity.enums.ResultadoAtencion;
import com.pe.swcotoschero.prospectos.Repository.AsignacionRepository;
import com.pe.swcotoschero.prospectos.Repository.CargaMasivaRepository;
import com.pe.swcotoschero.prospectos.Repository.ContactoRepository;
import com.pe.swcotoschero.prospectos.Repository.UsuarioRepository;
import com.pe.swcotoschero.prospectos.dto.reporte.BaseResumenDTO;
import com.pe.swcotoschero.prospectos.dto.reporte.DashboardDTO;
import com.pe.swcotoschero.prospectos.dto.reporte.DrillDownAsignacionDTO;
import com.pe.swcotoschero.prospectos.dto.reporte.EmbudoDTO;
import com.pe.swcotoschero.prospectos.dto.reporte.MetricasPeriodoDTO;
import com.pe.swcotoschero.prospectos.dto.reporte.RankingColaboradorDTO;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Logica de negocio para todos los reportes del dueno (RF-18, §5e/§5f MVP).
 *
 * Zona horaria: America/Lima (UTC-5, sin DST) — identica a ColaboradorColaService.
 * Todas las metricas de "dia" usan [00:00:00, 23:59:59.999999999] del dia Lima.
 * Las de "mes" usan [dia 1 00:00:00, ultimo instante del mes].
 *
 * Atribucion de ventas/derivaciones:
 *   Si Asignacion.derivadoPor != null -> atribuir a derivadoPor.
 *   Si null -> atribuir a usuario (el colaborador que llevo la gestion).
 */
@Service
@Transactional(readOnly = true)
public class ReportesService {

    private static final Logger log = LoggerFactory.getLogger(ReportesService.class);
    private static final ZoneId ZONA_LIMA = ZoneId.of("America/Lima");
    private static final long ADMIN_ROL_ID = 1L;

    private final AsignacionRepository asignacionRepository;
    private final ContactoRepository contactoRepository;
    private final CargaMasivaRepository cargaMasivaRepository;
    private final UsuarioRepository usuarioRepository;
    private final ColaboradorColaService colaboradorColaService;

    public ReportesService(AsignacionRepository asignacionRepository,
                           ContactoRepository contactoRepository,
                           CargaMasivaRepository cargaMasivaRepository,
                           UsuarioRepository usuarioRepository,
                           ColaboradorColaService colaboradorColaService) {
        this.asignacionRepository = asignacionRepository;
        this.contactoRepository = contactoRepository;
        this.cargaMasivaRepository = cargaMasivaRepository;
        this.usuarioRepository = usuarioRepository;
        this.colaboradorColaService = colaboradorColaService;
    }

    // =========================================================================
    // Dashboard principal (RF-18 §5e)
    // =========================================================================

    /**
     * Construye el dashboard completo del dueno.
     * Todas las queries son de solo lectura y se ejecutan en una unica transaccion.
     */
    public DashboardDTO calcularDashboard() {
        LocalDateTime ahora = LocalDateTime.now(ZONA_LIMA);
        LocalDate hoy = ahora.toLocalDate();

        // Rangos "dia"
        LocalDateTime inicioDia = hoy.atStartOfDay();
        LocalDateTime finDia = hoy.atTime(LocalTime.MAX);

        // Rangos "mes"
        LocalDate primerDelMes = hoy.withDayOfMonth(1);
        LocalDateTime inicioMes = primerDelMes.atStartOfDay();
        LocalDateTime finMes = finDia; // hasta el instante actual del dia de hoy (inclusive)

        List<Usuario> colaboradores = usuarioRepository.findActiveUsersWithoutAdminRole(ADMIN_ROL_ID);

        MetricasPeriodoDTO metricasDia = calcularMetricasDia(inicioDia, finDia, colaboradores);
        MetricasPeriodoDTO metricasMes = calcularMetricasMes(inicioMes, finMes, inicioDia, finDia);
        List<RankingColaboradorDTO> ranking = calcularRanking(colaboradores, inicioMes, finMes);
        EmbudoDTO embudo = calcularEmbudo();
        long porCerrar = asignacionRepository.countDerivadosGlobal();
        List<BaseResumenDTO> bases = calcularBases();

        return DashboardDTO.builder()
                .dia(metricasDia)
                .mes(metricasMes)
                .ranking(ranking)
                .embudo(embudo)
                .porCerrar(porCerrar)
                .bases(bases)
                .build();
    }

    private MetricasPeriodoDTO calcularMetricasDia(LocalDateTime inicioDia,
                                                    LocalDateTime finDia,
                                                    List<Usuario> colaboradores) {
        long ventasDia = asignacionRepository.countVentasCerradasPeriodo(inicioDia, finDia);
        long derivadosDia = asignacionRepository.countDerivadosPeriodo(inicioDia, finDia);
        long atencionesDia = contactoRepository.countAtencionesPeriodo(inicioDia, finDia);

        List<Object[]> contactabilidadRows = contactoRepository.contactabilidadGlobal(inicioDia, finDia);
        double contactabilidadReal = calcularContactabilidad(
                contactabilidadRows.isEmpty() ? null : contactabilidadRows.get(0));

        long colaboradoresActivos = contactoRepository.countColaboradoresActivosHoy(inicioDia, finDia);
        long colaboradoresTotal = colaboradores.size();
        long citasHoy = asignacionRepository.countCitasHoy(inicioDia, finDia);

        return MetricasPeriodoDTO.builder()
                .ventasCerradas(ventasDia)
                .derivados(derivadosDia)
                .atenciones(atencionesDia)
                .contactabilidadReal(contactabilidadReal)
                .colaboradoresActivos(colaboradoresActivos)
                .colaboradoresTotal(colaboradoresTotal)
                .citasHoy(citasHoy)
                .build();
    }

    private MetricasPeriodoDTO calcularMetricasMes(LocalDateTime inicioMes,
                                                    LocalDateTime finMes,
                                                    LocalDateTime inicioDia,
                                                    LocalDateTime finDia) {
        long ventasMes = asignacionRepository.countVentasCerradasPeriodo(inicioMes, finMes);
        long derivadosMes = asignacionRepository.countDerivadosPeriodo(inicioMes, finMes);
        long atencionesMes = contactoRepository.countAtencionesPeriodo(inicioMes, finMes);

        double tasaConversion = derivadosMes > 0
                ? (double) ventasMes / derivadosMes
                : 0.0;

        long totalAsignaciones = asignacionRepository.countTotalAsignaciones();
        long avanzados = asignacionRepository.countAvanceBasesMgmt();
        double avanceBasesPct = totalAsignaciones > 0
                ? (double) avanzados / totalAsignaciones
                : 0.0;

        long disponiblesSinAsignar = asignacionRepository.countProspectosSinAsignacion();

        return MetricasPeriodoDTO.builder()
                .ventasCerradas(ventasMes)
                .derivados(derivadosMes)
                .atenciones(atencionesMes)
                .tasaConversion(tasaConversion)
                .avanceBasesPct(avanceBasesPct)
                .disponiblesSinAsignar(disponiblesSinAsignar)
                .build();
    }

    private List<RankingColaboradorDTO> calcularRanking(List<Usuario> colaboradores,
                                                         LocalDateTime inicioMes,
                                                         LocalDateTime finMes) {
        List<RankingColaboradorDTO> ranking = new ArrayList<>();
        for (Usuario u : colaboradores) {
            Long uid = u.getId();

            long ventas = asignacionRepository.countVentasCerradasColaboradorPeriodo(uid, inicioMes, finMes);
            long derivados = asignacionRepository.countDerivadosColaboradorPeriodo(uid, inicioMes, finMes);
            long atenciones = contactoRepository.countAtencionesColaborador(uid, inicioMes, finMes);

            // Contactabilidad del colaborador en el mes
            long titular = contactoRepository.countTitularColaborador(uid, inicioMes, finMes);
            long conResultado = contactoRepository.countConResultadoColaborador(uid, inicioMes, finMes);
            double contactabilidad = conResultado > 0 ? (double) titular / conResultado : 0.0;
            // (contactabilidadColaborador query also available as List<Object[]> for batch use)

            LocalDateTime ultimaActividad = contactoRepository.ultimaActividadColaborador(uid);

            ranking.add(RankingColaboradorDTO.builder()
                    .usuarioId(uid)
                    .nombre(nombreCompleto(u.getNombre(), u.getApellidos()))
                    .ventasCerradas(ventas)
                    .derivados(derivados)
                    .atenciones(atenciones)
                    .contactabilidad(contactabilidad)
                    .ultimaActividad(ultimaActividad)
                    .build());
        }
        // Ordenar por ventas desc, luego derivados desc
        ranking.sort((a, b) -> {
            int cmp = Long.compare(b.getVentasCerradas(), a.getVentasCerradas());
            if (cmp != 0) return cmp;
            return Long.compare(b.getDerivados(), a.getDerivados());
        });
        return ranking;
    }

    private EmbudoDTO calcularEmbudo() {
        long asignados = asignacionRepository.countTotalAsignaciones();
        long gestionados = asignacionRepository.countGestionados();
        long contactadosTitular = asignacionRepository.countAsignacionesContactadasTitular();
        long interesados = asignacionRepository.countInteresados();
        long derivados = asignacionRepository.countDerivadosGlobal();
        long ventas = asignacionRepository.countVentasGlobal();

        return EmbudoDTO.builder()
                .asignados(asignados)
                .gestionados(gestionados)
                .contactadosTitular(contactadosTitular)
                .interesados(interesados)
                .derivados(derivados)
                .ventas(ventas)
                .build();
    }

    private List<BaseResumenDTO> calcularBases() {
        List<CargaMasiva> cargas = cargaMasivaRepository.findAllByOrderByFechaDesc();
        List<BaseResumenDTO> resultado = new ArrayList<>();
        for (CargaMasiva cm : cargas) {
            long cantidad = cm.getCantidadProspectos() != null ? cm.getCantidadProspectos() : 0L;
            long asignados = asignacionRepository.countProspectosAsignadosPorCarga(cm.getId());
            long sinAsignar = Math.max(cantidad - asignados, 0);
            double avancePct = cantidad > 0 ? (double) asignados / cantidad : 0.0;

            resultado.add(BaseResumenDTO.builder()
                    .id(cm.getId())
                    .nombre(cm.getNombrearchivo())
                    .cantidad(cantidad)
                    .asignados(asignados)
                    .sinAsignar(sinAsignar)
                    .avancePct(avancePct)
                    .build());
        }
        return resultado;
    }

    // =========================================================================
    // Drill-down del colaborador
    // =========================================================================

    /**
     * Devuelve las asignaciones paginadas de un colaborador con datos enmascarados.
     *
     * @param usuarioId     ID del colaborador
     * @param pagina        1-based
     * @param tamanioPagina Registros por pagina
     */
    public Map<String, Object> drillDownColaborador(Long usuarioId, int pagina, int tamanioPagina) {
        Usuario colaborador = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Colaborador no encontrado: " + usuarioId));

        PageRequest pageRequest = PageRequest.of(Math.max(pagina - 1, 0), tamanioPagina);
        Page<Asignacion> page = asignacionRepository.findByUsuarioPaginado(usuarioId, pageRequest);

        List<DrillDownAsignacionDTO> resultados = page.getContent().stream()
                .map(this::mapearDrillDown)
                .collect(Collectors.toList());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("colaborador", Map.of(
                "id", colaborador.getId(),
                "nombre", nombreCompleto(colaborador.getNombre(), colaborador.getApellidos())));
        response.put("resultados", resultados);
        response.put("pagina", page.getNumber() + 1);
        response.put("tamanioPagina", page.getSize());
        response.put("total", page.getTotalElements());
        response.put("totalPaginas", page.getTotalPages());
        return response;
    }

    private DrillDownAsignacionDTO mapearDrillDown(Asignacion a) {
        var p = a.getProspecto();
        long totalContactos = contactoRepository.countPorAsignacion(a.getAsignacionID());

        return DrillDownAsignacionDTO.builder()
                .asignacionId(a.getAsignacionID())
                .prospectoId(p.getProspectoID())
                .nombreProspecto(nombreCompleto(p.getNombre(), p.getApellido()))
                .celular(ColaboradorColaService.enmascararSensible(p.getCelular()))
                .documentoIdentidad(ColaboradorColaService.enmascararSensible(p.getDocumentoIdentidad()))
                .campania(p.getCampania() != null ? p.getCampania().getNombre() : null)
                .estado(a.getEstado() != null ? a.getEstado().name() : null)
                .estadoResultado(a.getEstadoResultado() != null ? a.getEstadoResultado().name() : null)
                .fechaAgenda(a.getFechaAgenda())
                .fechaAsignacion(a.getFechaAsignacion())
                .fechaCierre(a.getFechaCierre())
                .totalContactos(totalContactos)
                .build();
    }

    // =========================================================================
    // Exportacion Excel — prospectos (admin, sin enmascarar)
    // =========================================================================

    /**
     * Genera un .xlsx con todas las asignaciones que cumplen los filtros.
     * Datos SIN enmascarar (exportacion interna del dueno).
     *
     * @param campaniaNombre Filtro por nombre de campania (null = todos)
     * @param estadoStr      Filtro por EstadoGestion (null/blank = todos)
     * @param estadoResStr   Filtro por ResultadoAtencion (null/blank = todos)
     */
    public byte[] exportarProspectos(String campaniaNombre,
                                      String estadoStr,
                                      String estadoResStr) throws IOException {
        EstadoGestion estado = parseEnum(EstadoGestion.class, estadoStr);
        ResultadoAtencion estadoRes = parseEnum(ResultadoAtencion.class, estadoResStr);
        String campania = (campaniaNombre == null || campaniaNombre.isBlank()) ? null : campaniaNombre;

        List<Asignacion> asignaciones = asignacionRepository.findParaExportacion(campania, estado, estadoRes);

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Prospectos");

            CellStyle headerStyle = crearEstiloEncabezado(wb);

            String[] headers = {
                "Prospecto", "Documento", "Celular", "Campaña",
                "Colaborador", "Estado", "Resultado", "FechaAsignacion", "FechaCierre"
            };
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (Asignacion a : asignaciones) {
                var p = a.getProspecto();
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(nombreCompleto(p.getNombre(), p.getApellido()));
                row.createCell(1).setCellValue(nvl(p.getDocumentoIdentidad()));
                row.createCell(2).setCellValue(nvl(p.getCelular()));
                row.createCell(3).setCellValue(p.getCampania() != null ? p.getCampania().getNombre() : "");
                row.createCell(4).setCellValue(
                        a.getUsuario() != null
                                ? nombreCompleto(a.getUsuario().getNombre(), a.getUsuario().getApellidos())
                                : "");
                row.createCell(5).setCellValue(a.getEstado() != null ? a.getEstado().name() : "");
                row.createCell(6).setCellValue(
                        a.getEstadoResultado() != null ? a.getEstadoResultado().name() : "");
                row.createCell(7).setCellValue(
                        a.getFechaAsignacion() != null ? a.getFechaAsignacion().toString() : "");
                row.createCell(8).setCellValue(
                        a.getFechaCierre() != null ? a.getFechaCierre().toString() : "");
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        }
    }

    // =========================================================================
    // Exportacion Excel — mis prospectos (colaborador, enmascarado)
    // =========================================================================

    /**
     * Genera un .xlsx con la cola del colaborador segun el filtro.
     * Datos con celular/documento enmascarados (ultimos 3 visibles).
     *
     * @param usuarioId  ID del colaborador autenticado
     * @param filtroStr  Filtro de cola (FiltroColaborador)
     */
    public byte[] exportarMisProspectos(Long usuarioId, String filtroStr) throws IOException {
        // Delegamos la logica de filtrado a ColaboradorColaService (reutilizacion real)
        // Pedimos hasta 5000 registros para no saturar la memoria en un export razonable
        Map<String, Object> colaData = colaboradorColaService.obtenerCola(
                usuarioId, filtroStr, null, 1, 5000);

        @SuppressWarnings("unchecked")
        List<?> resultados = (List<?>) colaData.get("resultados");

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("MisProspectos");
            CellStyle headerStyle = crearEstiloEncabezado(wb);

            String[] headers = {
                "Prospecto", "Documento", "Celular", "Campaña",
                "Estado", "Resultado", "FechaAgenda", "TotalContactos"
            };
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (Object item : resultados) {
                // MiProspectoDTO — accedemos via reflection-safe cast
                if (item instanceof com.pe.swcotoschero.prospectos.dto.MiProspectoDTO dto) {
                    Row row = sheet.createRow(rowIdx++);
                    row.createCell(0).setCellValue(nombreCompleto(dto.getNombre(), dto.getApellido()));
                    row.createCell(1).setCellValue(nvl(dto.getDocumentoIdentidad()));
                    row.createCell(2).setCellValue(nvl(dto.getCelular()));
                    row.createCell(3).setCellValue(nvl(dto.getCampania()));
                    row.createCell(4).setCellValue(nvl(dto.getEstado()));
                    row.createCell(5).setCellValue(nvl(dto.getEstadoResultado()));
                    row.createCell(6).setCellValue(
                            dto.getFechaAgenda() != null ? dto.getFechaAgenda().toString() : "");
                    row.createCell(7).setCellValue(dto.getTotalContactos());
                }
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        }
    }

    // =========================================================================
    // Utilidades privadas
    // =========================================================================

    private double calcularContactabilidad(Object[] data) {
        if (data == null || data[0] == null || data[1] == null) return 0.0;
        long titular = toLong(data[0]);
        long conResultado = toLong(data[1]);
        return conResultado > 0 ? (double) titular / conResultado : 0.0;
    }

    private long toLong(Object val) {
        if (val == null) return 0L;
        if (val instanceof Long l) return l;
        if (val instanceof Number n) return n.longValue();
        return 0L;
    }

    private <E extends Enum<E>> E parseEnum(Class<E> clazz, String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Enum.valueOf(clazz, value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Valor '" + value + "' no es valido para " + clazz.getSimpleName());
        }
    }

    private CellStyle crearEstiloEncabezado(XSSFWorkbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    private static String nombreCompleto(String nombre, String apellido) {
        String n = nombre != null ? nombre.trim() : "";
        String a = apellido != null ? apellido.trim() : "";
        return (n + " " + a).trim();
    }

    private static String nvl(String val) {
        return val != null ? val : "";
    }
}
