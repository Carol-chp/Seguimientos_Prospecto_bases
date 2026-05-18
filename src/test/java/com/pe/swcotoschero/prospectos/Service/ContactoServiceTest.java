package com.pe.swcotoschero.prospectos.Service;

import com.pe.swcotoschero.prospectos.Entity.Asignacion;
import com.pe.swcotoschero.prospectos.Entity.ConfiguracionDueno;
import com.pe.swcotoschero.prospectos.Entity.Contacto;
import com.pe.swcotoschero.prospectos.Entity.Prospecto;
import com.pe.swcotoschero.prospectos.Entity.Rol;
import com.pe.swcotoschero.prospectos.Entity.Usuario;
import com.pe.swcotoschero.prospectos.Entity.enums.EstadoGestion;
import com.pe.swcotoschero.prospectos.Entity.enums.ResultadoAtencion;
import com.pe.swcotoschero.prospectos.Entity.enums.VerificacionSbs;
import com.pe.swcotoschero.prospectos.Repository.AperturaEventoRepository;
import com.pe.swcotoschero.prospectos.Repository.AsignacionRepository;
import com.pe.swcotoschero.prospectos.Repository.ConfiguracionDuenoRepository;
import com.pe.swcotoschero.prospectos.Repository.ContactoRepository;
import com.pe.swcotoschero.prospectos.Repository.ProspectoRepository;
import com.pe.swcotoschero.prospectos.dto.ContactoRegistroDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContactoServiceTest {

    @Mock
    private ContactoRepository contactoRepository;

    @Mock
    private AsignacionRepository asignacionRepository;

    @Mock
    private ProspectoRepository prospectoRepository;

    @Mock
    private AperturaEventoRepository aperturaEventoRepository;

    @Mock
    private ConfiguracionDuenoRepository configuracionDuenoRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private AsistenciaService asistenciaService;

    @InjectMocks
    private ContactoService contactoService;

    private Asignacion asignacionActiva;
    private Prospecto prospecto;
    private Usuario usuario;
    private ConfiguracionDueno config;

    @BeforeEach
    void setUp() {
        prospecto = new Prospecto();
        prospecto.setNombre("Juan");
        prospecto.setApellido("Pérez");
        prospecto.setCelular("999000111");

        Rol rol = new Rol();
        rol.setId(2L);
        rol.setNombre("TELEOPERADOR");

        usuario = new Usuario();
        usuario.setId(1L);
        usuario.setNombre("Ana");
        usuario.setApellidos("García");
        usuario.setRol(rol);

        asignacionActiva = new Asignacion();
        asignacionActiva.setProspecto(prospecto);
        asignacionActiva.setUsuario(usuario);
        asignacionActiva.setEstado(EstadoGestion.SIN_GESTIONAR);
        asignacionActiva.setVerificacionSbs(VerificacionSbs.APTO);
        asignacionActiva.setIntentosFallidos(0);

        config = new ConfiguracionDueno();
        config.setMaxIntentosNoContesto(6);
        config.setReglaReintentoNoContesto("+3h,+24h,+48h,+72h,+120h");
    }

    // =========================================================================
    // SBS gating — must be APTO before registrarContacto
    // =========================================================================

    @Test
    void registrarContacto_sinVerificacionApto_lanzaIllegalArgument() {
        asignacionActiva.setVerificacionSbs(null); // no verificado

        when(asignacionRepository
                .findFirstByProspecto_ProspectoIDAndEstadoNotInOrderByFechaAsignacionDesc(
                        anyLong(), anyList()))
                .thenReturn(Optional.of(asignacionActiva));

        ContactoRegistroDTO dto = dto("INTERESADO", null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> contactoService.registrarContacto(dto, usuario));

        assertTrue(ex.getMessage().contains("Debe verificar SBS"),
                "Mensaje debe mencionar SBS");
        verify(contactoRepository, never()).save(any());
    }

    @Test
    void registrarContacto_sbsObservado_lanzaIllegalArgument() {
        asignacionActiva.setVerificacionSbs(VerificacionSbs.OBSERVADO);

        when(asignacionRepository
                .findFirstByProspecto_ProspectoIDAndEstadoNotInOrderByFechaAsignacionDesc(
                        anyLong(), anyList()))
                .thenReturn(Optional.of(asignacionActiva));

        ContactoRegistroDTO dto = dto("INTERESADO", null);

        assertThrows(IllegalArgumentException.class,
                () -> contactoService.registrarContacto(dto, usuario));

        verify(contactoRepository, never()).save(any());
    }

    // =========================================================================
    // Agenda validation — AGENDADO / VOLVER_LLAMAR
    // =========================================================================

    @Test
    void registrarContacto_agendadoFechaPasada_lanzaIllegalArgument() {
        when(asignacionRepository
                .findFirstByProspecto_ProspectoIDAndEstadoNotInOrderByFechaAsignacionDesc(
                        anyLong(), anyList()))
                .thenReturn(Optional.of(asignacionActiva));
        when(configuracionDuenoRepository.findTopByOrderByIdAsc())
                .thenReturn(Optional.of(config));

        // past date
        String fechaPasada = LocalDateTime.now().minusDays(1)
                .withSecond(0).withNano(0).toString();

        ContactoRegistroDTO dto = dto("AGENDADO", fechaPasada);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> contactoService.registrarContacto(dto, usuario));

        assertTrue(ex.getMessage().toLowerCase().contains("futura"),
                "Mensaje debe indicar que la fecha debe ser futura");
    }

    @Test
    void registrarContacto_agendadoDiaNoLaborable_lanzaIllegalArgument() {
        when(asignacionRepository
                .findFirstByProspecto_ProspectoIDAndEstadoNotInOrderByFechaAsignacionDesc(
                        anyLong(), anyList()))
                .thenReturn(Optional.of(asignacionActiva));
        when(configuracionDuenoRepository.findTopByOrderByIdAsc())
                .thenReturn(Optional.of(config));
        when(asistenciaService.esDiaLaborable(any(LocalDate.class)))
                .thenReturn(false);

        String fechaFutura = LocalDateTime.now().plusDays(3)
                .withSecond(0).withNano(0).toString();

        ContactoRegistroDTO dto = dto("AGENDADO", fechaFutura);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> contactoService.registrarContacto(dto, usuario));

        assertTrue(ex.getMessage().toLowerCase().contains("laborable"),
                "Mensaje debe mencionar día laborable");
    }

    @Test
    void registrarContacto_agendadoFechaFuturaLaborable_estadoEnSeguimiento() {
        when(asignacionRepository
                .findFirstByProspecto_ProspectoIDAndEstadoNotInOrderByFechaAsignacionDesc(
                        anyLong(), anyList()))
                .thenReturn(Optional.of(asignacionActiva));
        when(configuracionDuenoRepository.findTopByOrderByIdAsc())
                .thenReturn(Optional.of(config));
        when(asistenciaService.esDiaLaborable(any(LocalDate.class)))
                .thenReturn(true);
        when(contactoRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(asignacionRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(prospectoRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        LocalDateTime futuro = LocalDateTime.now().plusDays(2).withSecond(0).withNano(0);
        // Use ISO pattern without seconds
        String fechaFutura = futuro.toLocalDate() + "T" + futuro.toLocalTime().withSecond(0).toString().substring(0, 5);

        ContactoRegistroDTO dto = dto("AGENDADO", fechaFutura);

        Map<String, Object> result = contactoService.registrarContacto(dto, usuario);

        assertEquals("EN_SEGUIMIENTO", result.get("estado"));
        assertNotNull(result.get("proximaLlamada"));

        ArgumentCaptor<Asignacion> captor = ArgumentCaptor.forClass(Asignacion.class);
        verify(asignacionRepository).save(captor.capture());
        assertEquals(EstadoGestion.EN_SEGUIMIENTO, captor.getValue().getEstado());
        assertNotNull(captor.getValue().getFechaAgenda());
    }

    // =========================================================================
    // NO_CONTESTO — retry logic
    // =========================================================================

    @Test
    void registrarContacto_noContesto_primerintento_estadoEnSeguimiento() {
        asignacionActiva.setIntentosFallidos(0);

        when(asignacionRepository
                .findFirstByProspecto_ProspectoIDAndEstadoNotInOrderByFechaAsignacionDesc(
                        anyLong(), anyList()))
                .thenReturn(Optional.of(asignacionActiva));
        when(configuracionDuenoRepository.findTopByOrderByIdAsc())
                .thenReturn(Optional.of(config));
        when(contactoRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(asignacionRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(prospectoRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        ContactoRegistroDTO dto = dto("NO_CONTESTO", null);
        dto.setSubmotivoNoContesto("BUZON");

        Map<String, Object> result = contactoService.registrarContacto(dto, usuario);

        assertEquals("EN_SEGUIMIENTO", result.get("estado"));
        assertNotNull(result.get("proximaLlamada"),
                "Primer intento debe programar próxima llamada");
    }

    @Test
    void registrarContacto_noContesto_excedeLimite_descartadoIlocalizable() {
        // Set intentos to max so next call exceeds limit
        config.setMaxIntentosNoContesto(3);
        asignacionActiva.setIntentosFallidos(3); // next will be 4 > 3

        when(asignacionRepository
                .findFirstByProspecto_ProspectoIDAndEstadoNotInOrderByFechaAsignacionDesc(
                        anyLong(), anyList()))
                .thenReturn(Optional.of(asignacionActiva));
        when(configuracionDuenoRepository.findTopByOrderByIdAsc())
                .thenReturn(Optional.of(config));
        when(contactoRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(asignacionRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(prospectoRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        ContactoRegistroDTO dto = dto("NO_CONTESTO", null);
        dto.setSubmotivoNoContesto("NO_CONTESTA");

        Map<String, Object> result = contactoService.registrarContacto(dto, usuario);

        assertEquals("DESCARTADO", result.get("estado"));
        assertNull(result.get("proximaLlamada"));

        ArgumentCaptor<Asignacion> captor = ArgumentCaptor.forClass(Asignacion.class);
        verify(asignacionRepository).save(captor.capture());
        assertEquals(EstadoGestion.DESCARTADO, captor.getValue().getEstado());
        assertEquals(ResultadoAtencion.ILOCALIZABLE, captor.getValue().getEstadoResultado());
        assertNull(captor.getValue().getFechaAgenda());
    }

    @Test
    void registrarContacto_noContesto_proximaLlamada_usaReglaReintento() {
        config.setReglaReintentoNoContesto("+3h,+24h,+48h");
        asignacionActiva.setIntentosFallidos(0);

        when(asignacionRepository
                .findFirstByProspecto_ProspectoIDAndEstadoNotInOrderByFechaAsignacionDesc(
                        anyLong(), anyList()))
                .thenReturn(Optional.of(asignacionActiva));
        when(configuracionDuenoRepository.findTopByOrderByIdAsc())
                .thenReturn(Optional.of(config));
        when(contactoRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(asignacionRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(prospectoRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        ContactoRegistroDTO dto = dto("NO_CONTESTO", null);
        dto.setSubmotivoNoContesto("OCUPADO");

        LocalDateTime before = LocalDateTime.now().plusHours(3).minusMinutes(1);
        Map<String, Object> result = contactoService.registrarContacto(dto, usuario);
        LocalDateTime after = LocalDateTime.now().plusHours(3).plusMinutes(1);

        // proximaLlamada should be roughly +3h
        String proxima = (String) result.get("proximaLlamada");
        assertNotNull(proxima);
        LocalDateTime parsed = LocalDateTime.parse(proxima);
        assertTrue(parsed.isAfter(before) && parsed.isBefore(after),
                "Próxima llamada debe ser +3h para el primer intento");
    }

    // =========================================================================
    // Result transitions
    // =========================================================================

    @Test
    void registrarContacto_interesado_estadoEnGestion() {
        when(asignacionRepository
                .findFirstByProspecto_ProspectoIDAndEstadoNotInOrderByFechaAsignacionDesc(
                        anyLong(), anyList()))
                .thenReturn(Optional.of(asignacionActiva));
        when(configuracionDuenoRepository.findTopByOrderByIdAsc())
                .thenReturn(Optional.of(config));
        when(contactoRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(asignacionRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(prospectoRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Map<String, Object> result = contactoService.registrarContacto(dto("INTERESADO", null), usuario);

        assertEquals("EN_GESTION", result.get("estado"));
        assertNull(result.get("proximaLlamada"));

        ArgumentCaptor<Asignacion> captor = ArgumentCaptor.forClass(Asignacion.class);
        verify(asignacionRepository).save(captor.capture());
        assertEquals(EstadoGestion.EN_GESTION, captor.getValue().getEstado());
        assertNull(captor.getValue().getFechaAgenda());
    }

    @Test
    void registrarContacto_derivado_estadoDerivadoConAtribucion() {
        when(asignacionRepository
                .findFirstByProspecto_ProspectoIDAndEstadoNotInOrderByFechaAsignacionDesc(
                        anyLong(), anyList()))
                .thenReturn(Optional.of(asignacionActiva));
        when(configuracionDuenoRepository.findTopByOrderByIdAsc())
                .thenReturn(Optional.of(config));
        when(contactoRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(asignacionRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(prospectoRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Map<String, Object> result = contactoService.registrarContacto(dto("DERIVADO", null), usuario);

        assertEquals("DERIVADO", result.get("estado"));

        ArgumentCaptor<Asignacion> captor = ArgumentCaptor.forClass(Asignacion.class);
        verify(asignacionRepository).save(captor.capture());
        Asignacion saved = captor.getValue();
        assertEquals(EstadoGestion.DERIVADO, saved.getEstado());
        assertEquals(usuario, saved.getDerivadoPor(), "derivadoPor debe ser el usuario autenticado");
        assertNotNull(saved.getFechaDerivacion(), "fechaDerivacion debe estar fijada");
        assertNull(saved.getFechaAgenda());
    }

    @Test
    void registrarContacto_noVolverLlamar_estadoDescartado() {
        when(asignacionRepository
                .findFirstByProspecto_ProspectoIDAndEstadoNotInOrderByFechaAsignacionDesc(
                        anyLong(), anyList()))
                .thenReturn(Optional.of(asignacionActiva));
        when(configuracionDuenoRepository.findTopByOrderByIdAsc())
                .thenReturn(Optional.of(config));
        when(contactoRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(asignacionRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(prospectoRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Map<String, Object> result = contactoService.registrarContacto(dto("NO_VOLVER_LLAMAR", null), usuario);

        assertEquals("DESCARTADO", result.get("estado"));
    }

    // =========================================================================
    // verificarSbs
    // =========================================================================

    @Test
    void verificarSbs_apto_continuarTrue() {
        when(asignacionRepository
                .findFirstByProspecto_ProspectoIDAndEstadoNotInOrderByFechaAsignacionDesc(
                        anyLong(), anyList()))
                .thenReturn(Optional.of(asignacionActiva));
        when(asignacionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var reqDto = new com.pe.swcotoschero.prospectos.dto.VerificacionSbsRequestDTO();
        reqDto.setProspectoId(1L);
        reqDto.setResultado("APTO");

        Map<String, Object> result = contactoService.verificarSbs(reqDto);

        assertTrue((Boolean) result.get("continuar"));
        assertFalse(result.containsKey("fechaReevaluacionSbs"));
        verify(contactoRepository, never()).save(any());
    }

    @Test
    void verificarSbs_observado_continuarFalseYCreaContactoHistorico() {
        when(asignacionRepository
                .findFirstByProspecto_ProspectoIDAndEstadoNotInOrderByFechaAsignacionDesc(
                        anyLong(), anyList()))
                .thenReturn(Optional.of(asignacionActiva));
        when(configuracionDuenoRepository.findTopByOrderByIdAsc())
                .thenReturn(Optional.of(config));
        when(asignacionRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(contactoRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var reqDto = new com.pe.swcotoschero.prospectos.dto.VerificacionSbsRequestDTO();
        reqDto.setProspectoId(1L);
        reqDto.setResultado("OBSERVADO");
        reqDto.setComentario("Tiene deuda");

        Map<String, Object> result = contactoService.verificarSbs(reqDto);

        assertFalse((Boolean) result.get("continuar"));
        assertEquals("EN_SEGUIMIENTO", result.get("estado"));
        assertNotNull(result.get("fechaReevaluacionSbs"),
                "Debe retornar fecha de reevaluación");

        // A Contacto history record must be saved
        verify(contactoRepository).save(any(Contacto.class));

        // Asignacion must transition to EN_SEGUIMIENTO
        ArgumentCaptor<Asignacion> captor = ArgumentCaptor.forClass(Asignacion.class);
        verify(asignacionRepository).save(captor.capture());
        assertEquals(EstadoGestion.EN_SEGUIMIENTO, captor.getValue().getEstado());
        assertNotNull(captor.getValue().getFechaReevaluacionSbs());
        assertNotNull(captor.getValue().getFechaAgenda(),
                "Agenda a las 09:00 del día de reevaluación");
    }

    @Test
    void verificarSbs_observado_fechaReevaluacionExplicita_usaFechaProvista() {
        when(asignacionRepository
                .findFirstByProspecto_ProspectoIDAndEstadoNotInOrderByFechaAsignacionDesc(
                        anyLong(), anyList()))
                .thenReturn(Optional.of(asignacionActiva));
        when(asignacionRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(contactoRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var reqDto = new com.pe.swcotoschero.prospectos.dto.VerificacionSbsRequestDTO();
        reqDto.setProspectoId(1L);
        reqDto.setResultado("OBSERVADO");
        reqDto.setFechaReevaluacion("2030-01-15");

        Map<String, Object> result = contactoService.verificarSbs(reqDto);

        assertEquals("2030-01-15", result.get("fechaReevaluacionSbs"));
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private ContactoRegistroDTO dto(String resultado, String fechaAgenda) {
        ContactoRegistroDTO d = new ContactoRegistroDTO();
        d.setProspectoId(1L);
        d.setResultado(resultado);
        d.setFechaAgenda(fechaAgenda);
        return d;
    }
}
