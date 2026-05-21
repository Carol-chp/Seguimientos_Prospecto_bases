package com.pe.swcotoschero.prospectos.Service;

import com.pe.swcotoschero.prospectos.Repository.AsignacionRepository;
import com.pe.swcotoschero.prospectos.Repository.CargaMasivaRepository;
import com.pe.swcotoschero.prospectos.Repository.ContactoRepository;
import com.pe.swcotoschero.prospectos.Repository.UsuarioRepository;
import com.pe.swcotoschero.prospectos.dto.reporte.DashboardDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportesServiceTest {

    @Mock
    private AsignacionRepository asignacionRepository;

    @Mock
    private ContactoRepository contactoRepository;

    @Mock
    private CargaMasivaRepository cargaMasivaRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private ColaboradorColaService colaboradorColaService;

    @Mock
    private AsistenciaService asistenciaService;

    @Mock
    private ReasignacionService reasignacionService;

    @InjectMocks
    private ReportesService reportesService;

    // =========================================================================
    // calcularDashboard — wires asistencia and porEnRiesgo
    // =========================================================================

    @Test
    void calcularDashboard_wireasistenciaYporEnRiesgo() {
        stubRepoMinimos();

        Map<String, Object> asistencia = Map.of(
                "fecha", "2026-05-18",
                "esLaborable", true,
                "totalColaboradores", 3,
                "totalAusentes", 1,
                "colaboradores", List.of());
        when(asistenciaService.asistenciaHoy()).thenReturn(asistencia);

        Map<String, Object> enRiesgo = Map.of("total", 2L, "resultados", List.of());
        when(reasignacionService.enRiesgo()).thenReturn(enRiesgo);

        DashboardDTO dto = reportesService.calcularDashboard();

        assertNotNull(dto);
        assertEquals(asistencia, dto.getAsistencia(),
                "asistencia debe ser el objeto retornado por AsistenciaService");
        assertEquals(2L, dto.getPorEnRiesgo(),
                "porEnRiesgo debe venir de ReasignacionService.enRiesgo().get(\"total\")");
    }

    @Test
    void calcularDashboard_reasignacionServiceLanzaExcepcion_porEnRiesgoCero() {
        stubRepoMinimos();
        when(asistenciaService.asistenciaHoy()).thenReturn(Map.of(
                "fecha", "2026-05-18",
                "esLaborable", false,
                "totalColaboradores", 0,
                "totalAusentes", 0,
                "colaboradores", List.of()));
        when(reasignacionService.enRiesgo())
                .thenThrow(new RuntimeException("DB unavailable"));

        // Must not propagate the exception
        DashboardDTO dto = assertDoesNotThrow(() -> reportesService.calcularDashboard());

        assertEquals(0L, dto.getPorEnRiesgo(),
                "porEnRiesgo debe ser 0 cuando ReasignacionService lanza excepción (defensa)");
    }

    @Test
    void calcularDashboard_porEnRiesgoConTotalNoNumerico_retornaCero() {
        stubRepoMinimos();
        when(asistenciaService.asistenciaHoy()).thenReturn(Map.of(
                "fecha", "2026-05-18",
                "esLaborable", false,
                "totalColaboradores", 0,
                "totalAusentes", 0,
                "colaboradores", List.of()));

        // Return a map without a numeric "total"
        when(reasignacionService.enRiesgo())
                .thenReturn(Map.of("total", 5L, "resultados", List.of()));

        DashboardDTO dto = reportesService.calcularDashboard();

        assertEquals(5L, dto.getPorEnRiesgo());
    }

    @Test
    void calcularDashboard_sinColaboradores_embudo_noLanza() {
        stubRepoMinimos();
        when(asistenciaService.asistenciaHoy()).thenReturn(Map.of(
                "fecha", "2026-05-18",
                "esLaborable", false,
                "totalColaboradores", 0,
                "totalAusentes", 0,
                "colaboradores", List.of()));
        when(reasignacionService.enRiesgo()).thenReturn(Map.of("total", 0, "resultados", List.of()));

        DashboardDTO dto = assertDoesNotThrow(() -> reportesService.calcularDashboard());

        assertNotNull(dto.getEmbudo());
        assertNotNull(dto.getDia());
        assertNotNull(dto.getMes());
        assertNotNull(dto.getRanking());
        assertTrue(dto.getRanking().isEmpty(), "Sin colaboradores, ranking debe estar vacío");
    }

    @Test
    void calcularDashboard_incluyeBasesSinCargas_listaVacia() {
        stubRepoMinimos();
        when(asistenciaService.asistenciaHoy()).thenReturn(Map.of(
                "fecha", "2026-05-18",
                "esLaborable", false,
                "totalColaboradores", 0,
                "totalAusentes", 0,
                "colaboradores", List.of()));
        when(reasignacionService.enRiesgo()).thenReturn(Map.of("total", 0, "resultados", List.of()));
        when(cargaMasivaRepository.findAllByOrderByFechaDesc()).thenReturn(Collections.emptyList());

        DashboardDTO dto = reportesService.calcularDashboard();

        assertNotNull(dto.getBases());
        assertTrue(dto.getBases().isEmpty());
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /**
     * Stubs all repository calls that ReportesService makes during calcularDashboard
     * to safe zero/empty defaults, so individual test cases can focus on what they care about.
     */
    private void stubRepoMinimos() {
        // Users
        when(usuarioRepository.findActiveUsersWithoutAdminRole(1L))
                .thenReturn(Collections.emptyList());

        // Period counts (dia + mes)
        when(asignacionRepository.countVentasCerradasPeriodo(any(), any())).thenReturn(0L);
        when(contactoRepository.countAtencionesPeriodo(any(), any())).thenReturn(0L);
        when(contactoRepository.contactabilidadGlobal(any(), any())).thenReturn(Collections.emptyList());
        when(contactoRepository.countColaboradoresActivosHoy(any(), any())).thenReturn(0L);
        when(asignacionRepository.countCitasHoy(any(), any())).thenReturn(0L);

        // Mes extras
        when(asignacionRepository.countTotalAsignaciones()).thenReturn(0L);
        when(asignacionRepository.countAvanceBasesMgmt()).thenReturn(0L);
        when(asignacionRepository.countProspectosSinAsignacion()).thenReturn(0L);

        // Embudo
        when(asignacionRepository.countGestionados()).thenReturn(0L);
        when(asignacionRepository.countAsignacionesContactadasTitular()).thenReturn(0L);
        when(asignacionRepository.countInteresados()).thenReturn(0L);
        when(asignacionRepository.countVentasGlobal()).thenReturn(0L);

        // Bases
        when(cargaMasivaRepository.findAllByOrderByFechaDesc()).thenReturn(Collections.emptyList());
    }
}
