package com.pe.swcotoschero.prospectos.Service;

import com.pe.swcotoschero.prospectos.Entity.ConfiguracionDueno;
import com.pe.swcotoschero.prospectos.Entity.Rol;
import com.pe.swcotoschero.prospectos.Entity.Usuario;
import com.pe.swcotoschero.prospectos.Repository.ConfiguracionDuenoRepository;
import com.pe.swcotoschero.prospectos.Repository.ContactoRepository;
import com.pe.swcotoschero.prospectos.Repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EmailService.
 *
 * Key design: mailEnabled is a @Value-injected private field.
 * We use ReflectionTestUtils.setField to control it without a Spring context.
 */
@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private ObjectProvider<JavaMailSender> mailSenderProvider;

    @Mock
    private ReportesService reportesService;

    @Mock
    private ConfiguracionDuenoRepository configRepo;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private ContactoRepository contactoRepository;

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        // Default: mail disabled
        ReflectionTestUtils.setField(emailService, "mailEnabled", false);
    }

    // =========================================================================
    // mailEnabled = false → never calls JavaMailSender
    // =========================================================================

    @Test
    void enviarResumenDiario_mailDisabled_noLlamaSender() {
        ReflectionTestUtils.setField(emailService, "mailEnabled", false);

        ConfiguracionDueno cfg = new ConfiguracionDueno();
        cfg.setId(1L);
        cfg.setToggleResumenDiario(true); // toggle ON but mail disabled
        when(configRepo.findTopByOrderByIdAsc()).thenReturn(Optional.of(cfg));
        when(mailSenderProvider.getIfAvailable()).thenReturn(null); // no sender

        EmailService.ResultadoEnvio result = emailService.enviarResumenDiario();

        assertFalse(result.enviado(), "No debe enviarse con mail desactivado");
        verifyNoInteractions(mailSender);
    }

    @Test
    void enviarResumenDiario_mailDisabled_nuncaLanzaExcepcion() {
        ReflectionTestUtils.setField(emailService, "mailEnabled", false);

        ConfiguracionDueno cfg = new ConfiguracionDueno();
        cfg.setId(1L);
        cfg.setToggleResumenDiario(true);
        when(configRepo.findTopByOrderByIdAsc()).thenReturn(Optional.of(cfg));

        // Must not throw even if sender were null
        assertDoesNotThrow(() -> emailService.enviarResumenDiario());
    }

    @Test
    void enviarResumenDiario_toggleResumenDiarioOff_noEnvia() {
        ReflectionTestUtils.setField(emailService, "mailEnabled", true);
        // NOTE: mailSenderProvider is NOT stubbed here because the toggle check
        // returns before the service ever asks for the sender.

        ConfiguracionDueno cfg = new ConfiguracionDueno();
        cfg.setId(1L);
        cfg.setToggleResumenDiario(false); // toggle off
        when(configRepo.findTopByOrderByIdAsc()).thenReturn(Optional.of(cfg));

        EmailService.ResultadoEnvio result = emailService.enviarResumenDiario();

        assertFalse(result.enviado());
        assertTrue(result.motivo().toLowerCase().contains("desactivado"),
                "Motivo debe indicar que el toggle está desactivado");
        verifyNoInteractions(mailSender);
    }

    @Test
    void enviarResumenDiario_sinDuenioEmail_noEnvia() {
        ReflectionTestUtils.setField(emailService, "mailEnabled", true);
        when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);

        ConfiguracionDueno cfg = new ConfiguracionDueno();
        cfg.setId(1L);
        cfg.setToggleResumenDiario(true);
        when(configRepo.findTopByOrderByIdAsc()).thenReturn(Optional.of(cfg));
        // No admin user found
        when(usuarioRepository.findByRol_IdAndEstadoOrderByNombreAsc(1L, true))
                .thenReturn(List.of());

        EmailService.ResultadoEnvio result = emailService.enviarResumenDiario();

        assertFalse(result.enviado());
        verifyNoInteractions(mailSender);
    }

    @Test
    void enviarResumenDiario_duenioSinEmail_noEnvia() {
        ReflectionTestUtils.setField(emailService, "mailEnabled", true);
        when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);

        ConfiguracionDueno cfg = new ConfiguracionDueno();
        cfg.setId(1L);
        cfg.setToggleResumenDiario(true);
        when(configRepo.findTopByOrderByIdAsc()).thenReturn(Optional.of(cfg));

        Rol rolAdmin = new Rol();
        rolAdmin.setId(1L);
        Usuario dueno = new Usuario();
        dueno.setId(1L);
        dueno.setNombre("Admin");
        dueno.setApellidos("Root");
        dueno.setEmail(""); // empty email
        dueno.setRol(rolAdmin);
        when(usuarioRepository.findByRol_IdAndEstadoOrderByNombreAsc(1L, true))
                .thenReturn(List.of(dueno));

        EmailService.ResultadoEnvio result = emailService.enviarResumenDiario();

        assertFalse(result.enviado());
        verifyNoInteractions(mailSender);
    }

    // =========================================================================
    // notificarAtencionAsync — best-effort gating when mail disabled
    // =========================================================================

    @Test
    void notificarAtencionAsync_mailDisabled_noLlamaSender_noLanzaExcepcion() {
        ReflectionTestUtils.setField(emailService, "mailEnabled", false);

        // Should be fire-and-forget, never throws
        assertDoesNotThrow(() -> emailService.notificarAtencionAsync(42L));

        // mailSenderProvider.getIfAvailable() must NOT be called since the gate
        // is the very first check
        verify(mailSenderProvider, never()).getIfAvailable();
    }

    @Test
    void notificarAtencionAsync_contactoIdNull_sinMailDesactivado_noLanza() {
        ReflectionTestUtils.setField(emailService, "mailEnabled", true);
        when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);
        // NOTE: configRepo is NOT stubbed because the null contactoId guard in the
        // service triggers after the sender check but before the config lookup:
        //   if (mailSender == null || contactoId == null) return;
        // So configRepo.findTopByOrderByIdAsc() is never called.

        assertDoesNotThrow(() -> emailService.notificarAtencionAsync(null));
        verifyNoInteractions(mailSender);
    }

    // =========================================================================
    // estadoUltimoEnvio — defensive when config is empty
    // =========================================================================

    @Test
    void estadoUltimoEnvio_sinConfiguracion_retornaDefaults() {
        ReflectionTestUtils.setField(emailService, "mailEnabled", false);
        when(configRepo.findTopByOrderByIdAsc()).thenReturn(Optional.empty());

        var result = emailService.estadoUltimoEnvio();

        assertNotNull(result);
        assertEquals(false, result.get("ok"));
        assertEquals(false, result.get("toggleResumenDiario"));
        assertEquals(false, result.get("mailConfigurado"));
    }
}
