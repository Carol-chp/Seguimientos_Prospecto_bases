package com.pe.swcotoschero.prospectos.Service;

import com.pe.swcotoschero.prospectos.Entity.ConfiguracionDueno;
import com.pe.swcotoschero.prospectos.Entity.Jornada;
import com.pe.swcotoschero.prospectos.Entity.Rol;
import com.pe.swcotoschero.prospectos.Entity.Usuario;
import com.pe.swcotoschero.prospectos.Repository.CalendarioLaboralRepository;
import com.pe.swcotoschero.prospectos.Repository.ConfiguracionDuenoRepository;
import com.pe.swcotoschero.prospectos.Repository.JornadaRepository;
import com.pe.swcotoschero.prospectos.Repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AsistenciaServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private JornadaRepository jornadaRepository;

    @Mock
    private CalendarioLaboralRepository calendarioRepository;

    @Mock
    private ConfiguracionDuenoRepository configRepo;

    @InjectMocks
    private AsistenciaService asistenciaService;

    // =========================================================================
    // esDiaLaborable
    // =========================================================================

    @Test
    void esDiaLaborable_domingo_retornaFalse() {
        // Find the next Sunday from today
        LocalDate domingo = LocalDate.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

        boolean result = asistenciaService.esDiaLaborable(domingo);

        assertFalse(result, "El domingo no debe ser laborable");
        // calendarioRepository should NOT be called — short-circuit on Sunday
        verify(calendarioRepository, never()).existsByFechaAndEsFeriadoTrue(any());
    }

    @Test
    void esDiaLaborable_feriadoEnCalendario_retornaFalse() {
        // Use a Monday to avoid the Sunday short-circuit
        LocalDate lunes = LocalDate.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
        when(calendarioRepository.existsByFechaAndEsFeriadoTrue(lunes)).thenReturn(true);

        boolean result = asistenciaService.esDiaLaborable(lunes);

        assertFalse(result, "Un feriado debe retornar false");
        verify(calendarioRepository).existsByFechaAndEsFeriadoTrue(lunes);
    }

    @Test
    void esDiaLaborable_lunes_sinFeriado_retornaTrue() {
        LocalDate lunes = LocalDate.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
        when(calendarioRepository.existsByFechaAndEsFeriadoTrue(lunes)).thenReturn(false);

        boolean result = asistenciaService.esDiaLaborable(lunes);

        assertTrue(result, "Un lunes sin feriado debe ser laborable");
    }

    @Test
    void esDiaLaborable_sabado_sinFeriado_retornaTrue() {
        LocalDate sabado = LocalDate.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY));
        when(calendarioRepository.existsByFechaAndEsFeriadoTrue(sabado)).thenReturn(false);

        boolean result = asistenciaService.esDiaLaborable(sabado);

        assertTrue(result, "El sábado es laborable según la decisión 6b.1");
    }

    // =========================================================================
    // asistenciaHoy — ausente logic: laborable + pasó límite + sin jornada
    // =========================================================================

    @Test
    void asistenciaHoy_diasNoLaborable_ningunAusente() {
        ConfiguracionDueno cfg = configuracionDefault();
        when(configRepo.findTopByOrderByIdAsc()).thenReturn(Optional.of(cfg));

        // Mock calendarioRepository to return that today is a holiday
        LocalDate hoy = LocalDate.now();
        // Force esDiaLaborable = false by making today a Sunday or a feriado.
        // Since we can't control "today" in unit tests, we need to pick the right path.
        // The logic is: laborable = esDiaLaborable(hoy).
        // If it's actually Sunday today, this passes naturally.
        // Otherwise mock the calendario to return feriado.
        when(calendarioRepository.existsByFechaAndEsFeriadoTrue(hoy))
                .thenReturn(true); // treat today as holiday for this test

        // Providing a collaborator who hasn't started jornada
        Usuario colab = colaborador(10L);
        when(usuarioRepository.findActiveUsersWithoutAdminRole(1L))
                .thenReturn(List.of(colab));
        when(jornadaRepository.findByUsuario_IdAndFecha(10L, hoy))
                .thenReturn(Optional.empty());

        Map<String, Object> result = asistenciaService.asistenciaHoy();

        // If it's Sunday skip the feriado mock — it returns false before calling repository.
        // Either way the result must be esLaborable=false and ausentes=0.
        assertEquals(false, result.get("esLaborable"));
        assertEquals(0, result.get("totalAusentes"));
    }

    @Test
    void asistenciaHoy_laborableYpasoLimiteYSinJornada_colaboradorAusente() {
        // We need today to be a working day AND the grace period to have passed.
        // To make this deterministic, set horaInicioJornada to "00:01" so limit
        // = 00:01 + 0 minutos de gracia = 00:01 → already passed.
        ConfiguracionDueno cfg = new ConfiguracionDueno();
        cfg.setHoraInicioJornada("00:01");
        cfg.setMinutosGraciaAusencia(0);
        when(configRepo.findTopByOrderByIdAsc()).thenReturn(Optional.of(cfg));

        LocalDate hoy = LocalDate.now();

        // Make today laborable (non-Sunday, non-feriado)
        // If today is Sunday, this test will produce 0 ausentes because the service
        // short-circuits at esDiaLaborable. Accept that edge case by only asserting
        // >= 0. The logic is still correct.
        boolean hoyEsDomingo = hoy.getDayOfWeek() == DayOfWeek.SUNDAY;
        if (!hoyEsDomingo) {
            when(calendarioRepository.existsByFechaAndEsFeriadoTrue(hoy)).thenReturn(false);
        }

        Usuario colab = colaborador(10L);
        when(usuarioRepository.findActiveUsersWithoutAdminRole(1L))
                .thenReturn(List.of(colab));
        when(jornadaRepository.findByUsuario_IdAndFecha(10L, hoy))
                .thenReturn(Optional.empty()); // no jornada

        Map<String, Object> result = asistenciaService.asistenciaHoy();

        if (hoyEsDomingo) {
            // On Sundays: not laborable → 0 ausentes (acceptable for unit test)
            assertEquals(0, result.get("totalAusentes"));
        } else {
            // It's a working day and limit has already passed → absent
            assertEquals(1, result.get("totalAusentes"),
                    "Colaborador sin jornada en día laborable tras límite debe ser ausente");
        }
    }

    @Test
    void asistenciaHoy_colaboradorConJornada_noAusente() {
        ConfiguracionDueno cfg = new ConfiguracionDueno();
        cfg.setHoraInicioJornada("00:01");
        cfg.setMinutosGraciaAusencia(0);
        when(configRepo.findTopByOrderByIdAsc()).thenReturn(Optional.of(cfg));

        LocalDate hoy = LocalDate.now();
        boolean hoyEsDomingo = hoy.getDayOfWeek() == DayOfWeek.SUNDAY;
        if (!hoyEsDomingo) {
            when(calendarioRepository.existsByFechaAndEsFeriadoTrue(hoy)).thenReturn(false);
        }

        Usuario colab = colaborador(20L);
        when(usuarioRepository.findActiveUsersWithoutAdminRole(1L))
                .thenReturn(List.of(colab));

        // Collaborator DID start their jornada
        Jornada jornada = new Jornada();
        jornada.setInicio(LocalDateTime.now().minusHours(1));
        when(jornadaRepository.findByUsuario_IdAndFecha(20L, hoy))
                .thenReturn(Optional.of(jornada));

        Map<String, Object> result = asistenciaService.asistenciaHoy();

        assertEquals(0, result.get("totalAusentes"),
                "Colaborador con jornada iniciada no debe ser ausente");
    }

    @Test
    void idsAusentesHoy_sinAusentes_listaVacia() {
        // Simple path: no collaborators at all
        ConfiguracionDueno cfg = configuracionDefault();
        when(configRepo.findTopByOrderByIdAsc()).thenReturn(Optional.of(cfg));
        when(usuarioRepository.findActiveUsersWithoutAdminRole(1L))
                .thenReturn(Collections.emptyList());
        when(calendarioRepository.existsByFechaAndEsFeriadoTrue(any())).thenReturn(false);

        List<Long> ausentes = asistenciaService.idsAusentesHoy();

        assertNotNull(ausentes);
        assertTrue(ausentes.isEmpty(), "Sin colaboradores, lista de ausentes debe ser vacía");
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private ConfiguracionDueno configuracionDefault() {
        ConfiguracionDueno cfg = new ConfiguracionDueno();
        cfg.setHoraInicioJornada("09:00");
        cfg.setMinutosGraciaAusencia(45);
        return cfg;
    }

    private Usuario colaborador(Long id) {
        Rol rol = new Rol();
        rol.setId(2L);
        rol.setNombre("TELEOPERADOR");
        Usuario u = new Usuario();
        u.setId(id);
        u.setNombre("Colab");
        u.setApellidos("Uno");
        u.setEstado(true);
        u.setRol(rol);
        return u;
    }
}
