package com.pe.swcotoschero.prospectos.Service;

import com.pe.swcotoschero.prospectos.Entity.Asignacion;
import com.pe.swcotoschero.prospectos.Entity.Banco;
import com.pe.swcotoschero.prospectos.Entity.ConfiguracionDueno;
import com.pe.swcotoschero.prospectos.Entity.Prospecto;
import com.pe.swcotoschero.prospectos.Entity.Usuario;
import com.pe.swcotoschero.prospectos.Entity.enums.EstadoGestion;
import com.pe.swcotoschero.prospectos.Entity.enums.VerificacionSbs;
import com.pe.swcotoschero.prospectos.Repository.AperturaEventoRepository;
import com.pe.swcotoschero.prospectos.Repository.AsignacionRepository;
import com.pe.swcotoschero.prospectos.Repository.ConfiguracionDuenoRepository;
import com.pe.swcotoschero.prospectos.Repository.ContactoRepository;
import com.pe.swcotoschero.prospectos.Repository.ProspectoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests para ContactoService.enviarABancoDestino (BK-2).
 *
 * Escenarios cubiertos:
 *  - Happy path: OBSERVADO + banco destino configurado → DESCARTADO + banco cambiado.
 *  - Rechazo: ciclo no OBSERVADO (APTO).
 *  - Rechazo: prospecto sin banco asignado.
 *  - Rechazo: banco sin banco destino (bancoDestino null).
 *  - Rechazo: ownership incorrecto (otro usuario).
 *  - Rechazo: sin ciclo activo.
 */
@ExtendWith(MockitoExtension.class)
class ContactoServiceEnviarBancoTest {

    @Mock private ContactoRepository contactoRepository;
    @Mock private AsignacionRepository asignacionRepository;
    @Mock private ProspectoRepository prospectoRepository;
    @Mock private AperturaEventoRepository aperturaEventoRepository;
    @Mock private ConfiguracionDuenoRepository configuracionDuenoRepository;
    @Mock private EmailService emailService;
    @Mock private AsistenciaService asistenciaService;

    @InjectMocks
    private ContactoService contactoService;

    private Banco scotiabank;
    private Banco bbva;
    private Prospecto prospecto;
    private Usuario usuario;
    private Asignacion asignacion;

    @BeforeEach
    void setUp() {
        bbva = new Banco();
        bbva.setId(2L);
        bbva.setNombre("BBVA");
        bbva.setActivo(true);
        bbva.setEsDefault(false);

        scotiabank = new Banco();
        scotiabank.setId(1L);
        scotiabank.setNombre("Scotiabank");
        scotiabank.setActivo(true);
        scotiabank.setEsDefault(true);
        scotiabank.setBancoDestino(bbva);

        prospecto = new Prospecto();
        prospecto.setNombre("María");
        prospecto.setApellido("López");
        prospecto.setBancoEntidad(scotiabank);

        usuario = new Usuario();
        usuario.setId(10L);
        usuario.setNombre("Carlos");
        usuario.setApellidos("Ríos");
        usuario.setBanco(scotiabank);

        asignacion = new Asignacion();
        asignacion.setProspecto(prospecto);
        asignacion.setUsuario(usuario);
        asignacion.setEstado(EstadoGestion.EN_SEGUIMIENTO);
        asignacion.setVerificacionSbs(VerificacionSbs.OBSERVADO);
    }

    // =========================================================================
    // Happy path
    // =========================================================================

    @Test
    void enviarABancoDestino_observado_cierraCicloYCambiaBanco() {
        when(asignacionRepository
                .findFirstByProspecto_ProspectoIDAndEstadoNotInOrderByFechaAsignacionDesc(
                        anyLong(), anyList()))
                .thenReturn(Optional.of(asignacion));
        when(asignacionRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(prospectoRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Map<String, Object> resultado = contactoService.enviarABancoDestino(1L, usuario.getId());

        assertTrue((Boolean) resultado.get("ok"));
        assertEquals("BBVA", resultado.get("bancoDestino"));

        // Verificar que la asignación quedó DESCARTADO
        ArgumentCaptor<Asignacion> capAsignacion = ArgumentCaptor.forClass(Asignacion.class);
        verify(asignacionRepository).save(capAsignacion.capture());
        assertEquals(EstadoGestion.DESCARTADO, capAsignacion.getValue().getEstado());
        assertNotNull(capAsignacion.getValue().getMotivoReasignacion());
        assertTrue(capAsignacion.getValue().getMotivoReasignacion().contains("BBVA"));
        assertNotNull(capAsignacion.getValue().getFechaReasignacion());
        assertNull(capAsignacion.getValue().getFechaAgenda());

        // Verificar que el prospecto cambió de banco
        ArgumentCaptor<Prospecto> capProspecto = ArgumentCaptor.forClass(Prospecto.class);
        verify(prospectoRepository).save(capProspecto.capture());
        assertEquals("BBVA", capProspecto.getValue().getBancoEntidad().getNombre());
    }

    // =========================================================================
    // Rechazos
    // =========================================================================

    @Test
    void enviarABancoDestino_noObservado_lanzaException() {
        asignacion.setVerificacionSbs(VerificacionSbs.APTO);

        when(asignacionRepository
                .findFirstByProspecto_ProspectoIDAndEstadoNotInOrderByFechaAsignacionDesc(
                        anyLong(), anyList()))
                .thenReturn(Optional.of(asignacion));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> contactoService.enviarABancoDestino(1L, usuario.getId()));

        assertTrue(ex.getMessage().contains("OBSERVADO"));
        verify(asignacionRepository, never()).save(any());
        verify(prospectoRepository, never()).save(any());
    }

    @Test
    void enviarABancoDestino_sinBancoEnProspecto_lanzaException() {
        prospecto.setBancoEntidad(null);
        asignacion.setVerificacionSbs(VerificacionSbs.OBSERVADO);

        when(asignacionRepository
                .findFirstByProspecto_ProspectoIDAndEstadoNotInOrderByFechaAsignacionDesc(
                        anyLong(), anyList()))
                .thenReturn(Optional.of(asignacion));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> contactoService.enviarABancoDestino(1L, usuario.getId()));

        assertTrue(ex.getMessage().toLowerCase().contains("banco"));
        verify(asignacionRepository, never()).save(any());
    }

    @Test
    void enviarABancoDestino_bancoSinDestino_lanzaException() {
        scotiabank.setBancoDestino(null);

        when(asignacionRepository
                .findFirstByProspecto_ProspectoIDAndEstadoNotInOrderByFechaAsignacionDesc(
                        anyLong(), anyList()))
                .thenReturn(Optional.of(asignacion));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> contactoService.enviarABancoDestino(1L, usuario.getId()));

        assertTrue(ex.getMessage().toLowerCase().contains("banco destino"));
        verify(asignacionRepository, never()).save(any());
    }

    @Test
    void enviarABancoDestino_otroUsuario_lanzaAccessDenied() {
        when(asignacionRepository
                .findFirstByProspecto_ProspectoIDAndEstadoNotInOrderByFechaAsignacionDesc(
                        anyLong(), anyList()))
                .thenReturn(Optional.of(asignacion));

        // Llamar con un ID distinto al dueño del ciclo
        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> contactoService.enviarABancoDestino(1L, 999L));

        verify(asignacionRepository, never()).save(any());
        verify(prospectoRepository, never()).save(any());
    }

    @Test
    void enviarABancoDestino_sinCicloActivo_lanzaException() {
        when(asignacionRepository
                .findFirstByProspecto_ProspectoIDAndEstadoNotInOrderByFechaAsignacionDesc(
                        anyLong(), anyList()))
                .thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> contactoService.enviarABancoDestino(1L, usuario.getId()));

        verify(asignacionRepository, never()).save(any());
    }
}
