package com.pe.swcotoschero.prospectos.Service;

import com.pe.swcotoschero.prospectos.Entity.Asignacion;
import com.pe.swcotoschero.prospectos.Entity.ConfiguracionDueno;
import com.pe.swcotoschero.prospectos.Entity.Prospecto;
import com.pe.swcotoschero.prospectos.Entity.Rol;
import com.pe.swcotoschero.prospectos.Entity.Usuario;
import com.pe.swcotoschero.prospectos.Entity.enums.EstadoGestion;
import com.pe.swcotoschero.prospectos.Entity.enums.ResultadoAtencion;
import com.pe.swcotoschero.prospectos.Repository.AsignacionRepository;
import com.pe.swcotoschero.prospectos.Repository.ConfiguracionDuenoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobDiarioServiceTest {

    @Mock
    private AsignacionRepository asignacionRepository;

    @Mock
    private ConfiguracionDuenoRepository configRepo;

    @InjectMocks
    private JobDiarioService jobDiarioService;

    private ConfiguracionDueno config;

    @BeforeEach
    void setUp() {
        config = new ConfiguracionDueno();
        config.setMaxIntentosNoContesto(6);
    }

    // =========================================================================
    // D7 — GANADO con fechaElegibilidad vencida → crea ciclo nuevo
    // =========================================================================

    @Test
    void ejecutar_ganadoElegible_creaCicloNuevo_ganadoIntacto() {
        Asignacion ganado = ganadoAsignacion(1L, LocalDate.now().minusDays(1));
        when(asignacionRepository.findGanadosReelegibles(any(LocalDate.class)))
                .thenReturn(List.of(ganado));
        when(asignacionRepository.findIlocalizablesPendientes(anyInt()))
                .thenReturn(Collections.emptyList());
        when(configRepo.findTopByOrderByIdAsc()).thenReturn(Optional.of(config));
        when(asignacionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Map<String, Object> result = jobDiarioService.ejecutar();

        assertEquals(1, result.get("ciclosNuevosD7"));
        assertEquals(0, result.get("ilocalizablesDescartados"));

        // Capture all saves
        ArgumentCaptor<Asignacion> captor = ArgumentCaptor.forClass(Asignacion.class);
        verify(asignacionRepository, times(2)).save(captor.capture());

        List<Asignacion> saves = captor.getAllValues();

        // First save = new cycle
        Asignacion nuevoCiclo = saves.get(0);
        assertEquals(EstadoGestion.SIN_GESTIONAR, nuevoCiclo.getEstado(),
                "Ciclo nuevo debe ser SIN_GESTIONAR");
        assertEquals(1L, nuevoCiclo.getCicloAnteriorId(),
                "cicloAnteriorId debe apuntar al GANADO original");
        assertEquals(0, nuevoCiclo.getIntentosFallidos(),
                "Intentos fallidos deben resetearse a 0");
        assertNull(nuevoCiclo.getVerificacionSbs(),
                "SBS debe quedar null (re-verificar)");
        assertEquals(ganado.getProspecto(), nuevoCiclo.getProspecto());
        assertEquals(ganado.getUsuario(), nuevoCiclo.getUsuario());
        assertNotNull(nuevoCiclo.getFechaAsignacion());

        // Second save = clearing fechaElegibilidad on GANADO record
        Asignacion ganadoActualizado = saves.get(1);
        assertNull(ganadoActualizado.getFechaElegibilidad(),
                "El GANADO original debe tener fechaElegibilidad=null tras procesarse");
        assertEquals(EstadoGestion.GANADO, ganadoActualizado.getEstado(),
                "El estado GANADO no debe cambiar (inmutable)");
    }

    @Test
    void ejecutar_ganadoElegible_noReactivaEnSeguimiento() {
        // This test asserts that a separate EN_SEGUIMIENTO is NOT touched by D7
        // D7 only processes GANADO with fechaElegibilidad vencida
        Asignacion ganado = ganadoAsignacion(2L, LocalDate.now().minusDays(1));
        when(asignacionRepository.findGanadosReelegibles(any(LocalDate.class)))
                .thenReturn(List.of(ganado));
        when(asignacionRepository.findIlocalizablesPendientes(anyInt()))
                .thenReturn(Collections.emptyList());
        when(configRepo.findTopByOrderByIdAsc()).thenReturn(Optional.of(config));
        when(asignacionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        jobDiarioService.ejecutar();

        // Only the GANADO and its new cycle are saved — no EN_SEGUIMIENTO touched
        verify(asignacionRepository, times(2)).save(any(Asignacion.class));
    }

    @Test
    void ejecutar_sinGanadosElegibles_ceroNuevosCiclos() {
        when(asignacionRepository.findGanadosReelegibles(any(LocalDate.class)))
                .thenReturn(Collections.emptyList());
        when(asignacionRepository.findIlocalizablesPendientes(anyInt()))
                .thenReturn(Collections.emptyList());
        when(configRepo.findTopByOrderByIdAsc()).thenReturn(Optional.of(config));

        Map<String, Object> result = jobDiarioService.ejecutar();

        assertEquals(0, result.get("ciclosNuevosD7"));
        assertEquals(0, result.get("ilocalizablesDescartados"));
        verify(asignacionRepository, never()).save(any());
    }

    // =========================================================================
    // Safety-net: EN_SEGUIMIENTO / NO_CONTESTO superó el máximo → DESCARTADO
    // =========================================================================

    @Test
    void ejecutar_redSeguridad_descartaIlocalizables() {
        Asignacion ilocalizable = new Asignacion();
        ilocalizable.setEstado(EstadoGestion.EN_SEGUIMIENTO);
        ilocalizable.setEstadoResultado(ResultadoAtencion.NO_CONTESTO);
        ilocalizable.setIntentosFallidos(10); // > max (6)
        ilocalizable.setFechaAgenda(LocalDateTime.now().minusDays(1));

        when(asignacionRepository.findGanadosReelegibles(any(LocalDate.class)))
                .thenReturn(Collections.emptyList());
        when(configRepo.findTopByOrderByIdAsc()).thenReturn(Optional.of(config));
        when(asignacionRepository.findIlocalizablesPendientes(6))
                .thenReturn(List.of(ilocalizable));
        when(asignacionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Map<String, Object> result = jobDiarioService.ejecutar();

        assertEquals(0, result.get("ciclosNuevosD7"));
        assertEquals(1, result.get("ilocalizablesDescartados"));

        ArgumentCaptor<Asignacion> captor = ArgumentCaptor.forClass(Asignacion.class);
        verify(asignacionRepository).save(captor.capture());

        Asignacion saved = captor.getValue();
        assertEquals(EstadoGestion.DESCARTADO, saved.getEstado());
        assertEquals(ResultadoAtencion.ILOCALIZABLE, saved.getEstadoResultado());
        assertNull(saved.getFechaAgenda());
    }

    @Test
    void ejecutar_retornaFechaEnResultado() {
        when(asignacionRepository.findGanadosReelegibles(any(LocalDate.class)))
                .thenReturn(Collections.emptyList());
        when(asignacionRepository.findIlocalizablesPendientes(anyInt()))
                .thenReturn(Collections.emptyList());
        when(configRepo.findTopByOrderByIdAsc()).thenReturn(Optional.of(config));

        Map<String, Object> result = jobDiarioService.ejecutar();

        assertNotNull(result.get("fecha"), "Resultado debe incluir fecha de ejecución");
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private Asignacion ganadoAsignacion(Long id, LocalDate fechaElegibilidad) {
        Rol rol = new Rol();
        rol.setId(1L);

        Usuario admin = new Usuario();
        admin.setId(99L);
        admin.setNombre("Admin");
        admin.setApellidos("X");
        admin.setEstado(true);
        admin.setRol(rol);

        Rol rolTele = new Rol();
        rolTele.setId(2L);
        Usuario colab = new Usuario();
        colab.setId(10L);
        colab.setNombre("Colab");
        colab.setApellidos("Y");
        colab.setEstado(true);
        colab.setRol(rolTele);

        Prospecto p = new Prospecto();
        p.setNombre("Cliente");
        p.setApellido("Uno");

        Asignacion a = new Asignacion();
        try {
            java.lang.reflect.Field f = Asignacion.class.getDeclaredField("asignacionID");
            f.setAccessible(true);
            f.set(a, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        a.setProspecto(p);
        a.setUsuario(colab);
        a.setAdministrador(admin);
        a.setEstado(EstadoGestion.GANADO);
        a.setFechaElegibilidad(fechaElegibilidad);
        return a;
    }
}
