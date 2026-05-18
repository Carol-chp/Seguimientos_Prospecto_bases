package com.pe.swcotoschero.prospectos.Service;

import com.pe.swcotoschero.prospectos.Entity.Asignacion;
import com.pe.swcotoschero.prospectos.Entity.Rol;
import com.pe.swcotoschero.prospectos.Entity.Usuario;
import com.pe.swcotoschero.prospectos.Entity.enums.EstadoGestion;
import com.pe.swcotoschero.prospectos.Repository.AsignacionRepository;
import com.pe.swcotoschero.prospectos.Repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReasignacionServiceTest {

    @Mock
    private AsignacionRepository asignacionRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private AsistenciaService asistenciaService;

    @InjectMocks
    private ReasignacionService reasignacionService;

    private Usuario admin;
    private Usuario destino;

    @BeforeEach
    void setUp() {
        Rol rolAdmin = new Rol();
        rolAdmin.setId(1L);
        rolAdmin.setNombre("ADMINISTRADOR");

        admin = new Usuario();
        admin.setId(99L);
        admin.setNombre("Admin");
        admin.setApellidos("Root");
        admin.setEstado(true);
        admin.setRol(rolAdmin);

        Rol rolTele = new Rol();
        rolTele.setId(2L);
        rolTele.setNombre("TELEOPERADOR");

        destino = new Usuario();
        destino.setId(50L);
        destino.setNombre("María");
        destino.setApellidos("López");
        destino.setEstado(true);
        destino.setRol(rolTele);
    }

    // =========================================================================
    // Happy path
    // =========================================================================

    @Test
    void reasignar_casosActivos_mueveCorectamente() {
        Asignacion a1 = asignacion(1L, EstadoGestion.SIN_GESTIONAR, 10L);
        Asignacion a2 = asignacion(2L, EstadoGestion.EN_GESTION, 10L);
        Asignacion a3 = asignacion(3L, EstadoGestion.EN_SEGUIMIENTO, 10L);

        when(usuarioRepository.findById(50L)).thenReturn(Optional.of(destino));
        when(asignacionRepository.findAllById(List.of(1L, 2L, 3L)))
                .thenReturn(List.of(a1, a2, a3));
        when(asignacionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Map<String, Object> result = reasignacionService.reasignar(
                List.of(1L, 2L, 3L), 50L, "Prueba", admin);

        assertEquals(true, result.get("ok"));
        assertEquals(3, result.get("reasignados"));
        assertEquals(50L, result.get("destinoId"));

        ArgumentCaptor<Asignacion> captor = ArgumentCaptor.forClass(Asignacion.class);
        verify(asignacionRepository, times(3)).save(captor.capture());

        for (Asignacion saved : captor.getAllValues()) {
            assertEquals(destino, saved.getUsuario(), "Usuario debe ser destino");
            assertEquals(50L, saved.getReasignadoParaId());
            assertEquals(10L, saved.getReasignadoDeId(), "reasignadoDeId = ID original");
            assertEquals("Prueba", saved.getMotivoReasignacion());
            assertNotNull(saved.getFechaReasignacion());
        }
    }

    @Test
    void reasignar_casoYaPertenecealDestino_seCuentaCero() {
        Asignacion a1 = asignacion(1L, EstadoGestion.SIN_GESTIONAR, 50L); // ya es del destino

        when(usuarioRepository.findById(50L)).thenReturn(Optional.of(destino));
        when(asignacionRepository.findAllById(List.of(1L)))
                .thenReturn(List.of(a1));

        Map<String, Object> result = reasignacionService.reasignar(
                List.of(1L), 50L, "Motivo", admin);

        // Case already belongs to destino → skipped (no save)
        assertEquals(0, result.get("reasignados"));
        verify(asignacionRepository, never()).save(any());
    }

    // =========================================================================
    // Guard: only ACTIVE states can be reasigned
    // =========================================================================

    @Test
    void reasignar_estadoDerivado_lanzaIllegalArgument() {
        Asignacion derivado = asignacion(5L, EstadoGestion.DERIVADO, 10L);

        when(usuarioRepository.findById(50L)).thenReturn(Optional.of(destino));
        when(asignacionRepository.findAllById(List.of(5L)))
                .thenReturn(List.of(derivado));

        assertThrows(IllegalArgumentException.class,
                () -> reasignacionService.reasignar(List.of(5L), 50L, "m", admin));

        verify(asignacionRepository, never()).save(any());
    }

    @Test
    void reasignar_estadoGanado_lanzaIllegalArgument() {
        Asignacion ganado = asignacion(6L, EstadoGestion.GANADO, 10L);

        when(usuarioRepository.findById(50L)).thenReturn(Optional.of(destino));
        when(asignacionRepository.findAllById(List.of(6L)))
                .thenReturn(List.of(ganado));

        assertThrows(IllegalArgumentException.class,
                () -> reasignacionService.reasignar(List.of(6L), 50L, "m", admin));
    }

    @Test
    void reasignar_estadoDescartado_lanzaIllegalArgument() {
        Asignacion descartado = asignacion(7L, EstadoGestion.DESCARTADO, 10L);

        when(usuarioRepository.findById(50L)).thenReturn(Optional.of(destino));
        when(asignacionRepository.findAllById(List.of(7L)))
                .thenReturn(List.of(descartado));

        assertThrows(IllegalArgumentException.class,
                () -> reasignacionService.reasignar(List.of(7L), 50L, "m", admin));
    }

    // =========================================================================
    // Guard: destino must be active and non-admin
    // =========================================================================

    @Test
    void reasignar_destinoInactivo_lanzaIllegalArgument() {
        destino.setEstado(false);
        when(usuarioRepository.findById(50L)).thenReturn(Optional.of(destino));

        assertThrows(IllegalArgumentException.class,
                () -> reasignacionService.reasignar(List.of(1L), 50L, "m", admin));
    }

    @Test
    void reasignar_destinoEsAdmin_lanzaIllegalArgument() {
        Rol rolAdmin = new Rol();
        rolAdmin.setId(1L); // ADMIN_ROL_ID = 1
        destino.setRol(rolAdmin);
        when(usuarioRepository.findById(50L)).thenReturn(Optional.of(destino));

        assertThrows(IllegalArgumentException.class,
                () -> reasignacionService.reasignar(List.of(1L), 50L, "m", admin));
    }

    @Test
    void reasignar_listaVacia_lanzaIllegalArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> reasignacionService.reasignar(Collections.emptyList(), 50L, "m", admin));

        verify(usuarioRepository, never()).findById(anyLong());
    }

    // =========================================================================
    // enRiesgo — sin ausentes
    // =========================================================================

    @Test
    void enRiesgo_sinAusentes_retornaTotalCero_conNota() {
        when(asistenciaService.idsAusentesHoy()).thenReturn(Collections.emptyList());

        Map<String, Object> result = reasignacionService.enRiesgo();

        assertEquals(0, result.get("total"));
        assertNotNull(result.get("nota"),
                "Debe incluir una nota explicativa cuando no hay ausentes");
        assertTrue(result.get("nota").toString().length() > 0);
        // Never queries assignments because there are no absent users
        verify(asignacionRepository, never()).findEnRiesgo(any(), any());
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private Asignacion asignacion(Long id, EstadoGestion estado, Long usuarioId) {
        Rol rol = new Rol();
        rol.setId(2L);
        Usuario u = new Usuario();
        u.setId(usuarioId);
        u.setNombre("Colab");
        u.setApellidos("X");
        u.setEstado(true);
        u.setRol(rol);

        Asignacion a = new Asignacion();
        // Set ID via reflection since there's no setter for asignacionID that's visible
        try {
            java.lang.reflect.Field f = Asignacion.class.getDeclaredField("asignacionID");
            f.setAccessible(true);
            f.set(a, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        a.setEstado(estado);
        a.setUsuario(u);
        return a;
    }
}
