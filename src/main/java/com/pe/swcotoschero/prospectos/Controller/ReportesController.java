package com.pe.swcotoschero.prospectos.Controller;

import com.pe.swcotoschero.prospectos.Entity.Usuario;
import com.pe.swcotoschero.prospectos.Repository.UsuarioRepository;
import com.pe.swcotoschero.prospectos.Service.BitacoraService;
import com.pe.swcotoschero.prospectos.Service.ReportesService;
import com.pe.swcotoschero.prospectos.dto.reporte.DashboardDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Reportes y metricas del dueno (RF-18, §5e/§5f MVP).
 *
 * Endpoints:
 *  GET /api/reportes/dashboard                      — solo ADMINISTRADOR
 *  GET /api/reportes/colaborador/{usuarioId}        — solo ADMINISTRADOR
 *  GET /api/reportes/exportar-prospectos            — solo ADMINISTRADOR
 *  GET /api/reportes/exportar-mis-prospectos        — TELEOPERADOR o ADMINISTRADOR autenticado
 */
@RestController
@RequestMapping("/api/reportes")
public class ReportesController {

    private static final Logger log = LoggerFactory.getLogger(ReportesController.class);
    private static final MediaType XLSX_MEDIA_TYPE =
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final ReportesService reportesService;
    private final UsuarioRepository usuarioRepository;
    private final BitacoraService bitacoraService;

    public ReportesController(ReportesService reportesService,
                               UsuarioRepository usuarioRepository,
                               BitacoraService bitacoraService) {
        this.reportesService = reportesService;
        this.usuarioRepository = usuarioRepository;
        this.bitacoraService = bitacoraService;
    }

    // =========================================================================
    // 1. Dashboard del dueno
    // =========================================================================

    /**
     * Catalogo completo de metricas §5e MVP.
     * Solo accesible para el rol ADMINISTRADOR.
     */
    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<DashboardDTO> getDashboard() {
        DashboardDTO dashboard = reportesService.calcularDashboard();
        return ResponseEntity.ok(dashboard);
    }

    // =========================================================================
    // 2. Drill-down de un colaborador
    // =========================================================================

    /**
     * Asignaciones paginadas de un colaborador especifico (vista del dueno).
     * Prospecto enmascarado (celular/doc, ultimos 3 visibles).
     *
     * @param usuarioId     ID del colaborador a inspeccionar
     * @param pagina        Pagina 1-based (default 1)
     * @param tamanioPagina Registros por pagina (default 10)
     */
    @GetMapping("/colaborador/{usuarioId}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Map<String, Object>> drillDownColaborador(
            @PathVariable Long usuarioId,
            @RequestParam(defaultValue = "1") int pagina,
            @RequestParam(defaultValue = "10") int tamanioPagina) {

        Map<String, Object> resultado = reportesService.drillDownColaborador(
                usuarioId, pagina, tamanioPagina);
        return ResponseEntity.ok(resultado);
    }

    // =========================================================================
    // 3. Exportar prospectos (admin, sin enmascarar)
    // =========================================================================

    /**
     * Descarga un .xlsx con todos los prospectos/asignaciones que cumplen los filtros.
     * Datos SIN enmascarar — uso exclusivo del dueno.
     *
     * @param campania       Nombre exacto de campania (opcional)
     * @param estado         EstadoGestion (opcional)
     * @param estadoResultado ResultadoAtencion (opcional)
     */
    @GetMapping("/exportar-prospectos")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<byte[]> exportarProspectos(
            @RequestParam(required = false) String campania,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String estadoResultado) {

        try {
            byte[] xlsx = reportesService.exportarProspectos(campania, estado, estadoResultado);
            return ResponseEntity.ok()
                    .contentType(XLSX_MEDIA_TYPE)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            ContentDisposition.attachment()
                                    .filename("prospectos_reporte.xlsx")
                                    .build()
                                    .toString())
                    .body(xlsx);
        } catch (IllegalArgumentException e) {
            log.warn("Parametros invalidos en exportar-prospectos: {}", e.getMessage());
            throw e; // GlobalExceptionHandler -> 400
        } catch (Exception e) {
            log.error("Error generando Excel de prospectos", e);
            throw new RuntimeException("Error generando el reporte Excel");
        }
    }

    // =========================================================================
    // 4. Exportar mis prospectos (colaborador autenticado, enmascarado)
    // =========================================================================

    /**
     * Descarga de prospectos del colaborador — solo ADMINISTRADOR (privacidad).
     * El acceso se restringe al dueño; el frontend ya no muestra el botón al colaborador.
     *
     * @param filtro Filtro de FiltroColaborador (default TODOS)
     */
    @GetMapping("/exportar-mis-prospectos")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<byte[]> exportarMisProspectos(
            @RequestParam(defaultValue = "TODOS") String filtro) {

        try {
            Usuario usuario = usuarioAutenticado();
            byte[] xlsx = reportesService.exportarMisProspectos(usuario.getId(), filtro);
            return ResponseEntity.ok()
                    .contentType(XLSX_MEDIA_TYPE)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            ContentDisposition.attachment()
                                    .filename("mis_prospectos.xlsx")
                                    .build()
                                    .toString())
                    .body(xlsx);
        } catch (IllegalArgumentException e) {
            log.warn("Parametros invalidos en exportar-mis-prospectos: {}", e.getMessage());
            throw e; // GlobalExceptionHandler -> 400
        } catch (Exception e) {
            log.error("Error generando Excel de mis prospectos", e);
            throw new RuntimeException("Error generando el reporte Excel");
        }
    }

    // =========================================================================
    // 5. Bitácora global de atenciones (RF-20 / §5h) — solo ADMINISTRADOR
    // =========================================================================

    /**
     * Bitácora paginada con filtros opcionales: rango de fechas (AAAA-MM-DD),
     * colaborador, campaña, base (cargaMasivaId), resultado, quién contestó.
     */
    @GetMapping("/bitacora")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Map<String, Object>> bitacora(
            @RequestParam(required = false) String desde,
            @RequestParam(required = false) String hasta,
            @RequestParam(required = false) Long colaboradorId,
            @RequestParam(required = false) String campania,
            @RequestParam(required = false) Long baseId,
            @RequestParam(required = false) String resultado,
            @RequestParam(required = false) String quienContesto,
            @RequestParam(defaultValue = "1") int pagina,
            @RequestParam(defaultValue = "50") int tamano) {

        return ResponseEntity.ok(bitacoraService.buscar(
                desde, hasta, colaboradorId, campania, baseId,
                resultado, quienContesto, pagina, tamano));
    }

    /** Export Excel del resultado filtrado de la bitácora. */
    @GetMapping("/bitacora/exportar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<byte[]> exportarBitacora(
            @RequestParam(required = false) String desde,
            @RequestParam(required = false) String hasta,
            @RequestParam(required = false) Long colaboradorId,
            @RequestParam(required = false) String campania,
            @RequestParam(required = false) Long baseId,
            @RequestParam(required = false) String resultado,
            @RequestParam(required = false) String quienContesto) {

        try {
            byte[] xlsx = bitacoraService.exportar(
                    desde, hasta, colaboradorId, campania, baseId, resultado, quienContesto);
            return ResponseEntity.ok()
                    .contentType(XLSX_MEDIA_TYPE)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            ContentDisposition.attachment()
                                    .filename("bitacora.xlsx")
                                    .build()
                                    .toString())
                    .body(xlsx);
        } catch (IllegalArgumentException e) {
            log.warn("Parametros invalidos en bitacora/exportar: {}", e.getMessage());
            throw e; // GlobalExceptionHandler -> 400
        } catch (Exception e) {
            log.error("Error generando Excel de bitacora", e);
            throw new RuntimeException("Error generando el reporte Excel");
        }
    }

    // =========================================================================
    // Utilidades
    // =========================================================================

    private Usuario usuarioAutenticado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return usuarioRepository.findByUsuarioAndEstado(auth.getName(), true)
                .orElseThrow(() -> new IllegalArgumentException("Usuario autenticado no encontrado"));
    }
}
