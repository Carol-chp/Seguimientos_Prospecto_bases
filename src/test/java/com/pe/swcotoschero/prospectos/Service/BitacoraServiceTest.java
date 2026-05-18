package com.pe.swcotoschero.prospectos.Service;

import com.pe.swcotoschero.prospectos.Entity.Asignacion;
import com.pe.swcotoschero.prospectos.Entity.Campania;
import com.pe.swcotoschero.prospectos.Entity.Contacto;
import com.pe.swcotoschero.prospectos.Entity.Prospecto;
import com.pe.swcotoschero.prospectos.Entity.Rol;
import com.pe.swcotoschero.prospectos.Entity.Usuario;
import com.pe.swcotoschero.prospectos.Entity.enums.ResultadoAtencion;
import com.pe.swcotoschero.prospectos.Repository.BitacoraRepository;
import com.pe.swcotoschero.prospectos.dto.reporte.BitacoraFilaDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BitacoraServiceTest {

    @Mock
    private BitacoraRepository bitacoraRepository;

    @InjectMocks
    private BitacoraService bitacoraService;

    // =========================================================================
    // Filter parsing — invalid inputs
    // =========================================================================

    @Test
    void buscar_fechaDesdeInvalida_lanzaIllegalArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> bitacoraService.buscar(
                        "NOT-A-DATE", null, null, null, null, null, null, 1, 10));
    }

    @Test
    void buscar_fechaHastaInvalida_lanzaIllegalArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> bitacoraService.buscar(
                        null, "31-13-2024", null, null, null, null, null, 1, 10));
    }

    @Test
    void buscar_resultadoEnumInvalido_lanzaIllegalArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> bitacoraService.buscar(
                        null, null, null, null, null, "RESULTADO_INEXISTENTE", null, 1, 10));
    }

    @Test
    void buscar_quienContestoEnumInvalido_lanzaIllegalArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> bitacoraService.buscar(
                        null, null, null, null, null, null, "FANTASMA", 1, 10));
    }

    @Test
    void buscar_hastaAntesQueDesde_lanzaIllegalArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> bitacoraService.buscar(
                        "2024-06-10", "2024-06-01", // hasta < desde
                        null, null, null, null, null, 1, 10));
    }

    // =========================================================================
    // Happy path — page metadata and celular masking
    // =========================================================================

    @Test
    void buscar_sinFiltros_retornaMetadatosDePaginaYFilas() {
        Contacto c = contacto("987654321", "INTERESADO");
        Page<Contacto> page = new PageImpl<>(List.of(c),
                org.springframework.data.domain.PageRequest.of(0, 10), 1L);

        when(bitacoraRepository.buscar(
                isNull(), isNull(), isNull(), isNull(), isNull(),
                isNull(), isNull(), any(Pageable.class)))
                .thenReturn(page);

        Map<String, Object> result = bitacoraService.buscar(
                null, null, null, null, null, null, null, 1, 10);

        assertEquals(1L, result.get("total"));
        assertEquals(1, result.get("totalPaginas"));
        assertEquals(1, result.get("pagina"));
        assertEquals(10, result.get("tamano"));

        @SuppressWarnings("unchecked")
        List<BitacoraFilaDTO> filas = (List<BitacoraFilaDTO>) result.get("resultados");
        assertNotNull(filas);
        assertEquals(1, filas.size());
    }

    @Test
    void buscar_celularEnmascarado_últimos3Visibles() {
        Contacto c = contacto("987654321", "NO_CONTESTO");
        Page<Contacto> page = new PageImpl<>(List.of(c),
                org.springframework.data.domain.PageRequest.of(0, 10), 1L);

        when(bitacoraRepository.buscar(any(), any(), any(), any(), any(), any(), any(),
                any(Pageable.class))).thenReturn(page);

        Map<String, Object> result = bitacoraService.buscar(
                null, null, null, null, null, null, null, 1, 10);

        @SuppressWarnings("unchecked")
        List<BitacoraFilaDTO> filas = (List<BitacoraFilaDTO>) result.get("resultados");
        String celular = filas.get(0).getCelular();

        // "987654321" → "******321"
        assertNotNull(celular);
        assertTrue(celular.endsWith("321"), "Últimos 3 dígitos deben ser visibles: " + celular);
        assertTrue(celular.startsWith("***"), "Los primeros dígitos deben estar enmascarados: " + celular);
    }

    @Test
    void buscar_campoResultadoMapeadoCorrectamente() {
        Contacto c = contacto("123", "INTERESADO");
        Page<Contacto> page = new PageImpl<>(List.of(c),
                org.springframework.data.domain.PageRequest.of(0, 10), 1L);

        when(bitacoraRepository.buscar(any(), any(), any(), any(), any(), any(), any(),
                any(Pageable.class))).thenReturn(page);

        Map<String, Object> result = bitacoraService.buscar(
                null, null, null, null, null, null, null, 1, 10);

        @SuppressWarnings("unchecked")
        List<BitacoraFilaDTO> filas = (List<BitacoraFilaDTO>) result.get("resultados");
        assertEquals("INTERESADO", filas.get(0).getEstadoResultado());
    }

    @Test
    void buscar_filtroFechaValida_pasaAlRepositorio() {
        Page<Contacto> emptyPage = new PageImpl<>(List.of(),
                org.springframework.data.domain.PageRequest.of(0, 10), 0L);

        when(bitacoraRepository.buscar(
                any(), any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(emptyPage);

        // Should not throw
        assertDoesNotThrow(() ->
                bitacoraService.buscar("2024-01-01", "2024-12-31", null, null, null, null, null, 1, 10));

        verify(bitacoraRepository).buscar(
                any(), any(), any(), any(), any(), any(), any(), any(Pageable.class));
    }

    @Test
    void buscar_resultadoFiltroValido_seResuelveCorrectamente() {
        Page<Contacto> emptyPage = new PageImpl<>(List.of(),
                org.springframework.data.domain.PageRequest.of(0, 10), 0L);

        when(bitacoraRepository.buscar(
                any(), any(), any(), any(), any(),
                eq(ResultadoAtencion.INTERESADO), any(), any(Pageable.class)))
                .thenReturn(emptyPage);

        assertDoesNotThrow(() ->
                bitacoraService.buscar(null, null, null, null, null, "INTERESADO", null, 1, 10));
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private Contacto contacto(String celular, String resultado) {
        Rol rol = new Rol();
        rol.setId(2L);

        Usuario u = new Usuario();
        u.setId(1L);
        u.setNombre("Ana");
        u.setApellidos("Ríos");
        u.setRol(rol);

        Campania camp = new Campania();
        camp.setNombre("Camp A");

        Prospecto p = new Prospecto();
        p.setNombre("Luis");
        p.setApellido("Torres");
        p.setCelular(celular);
        p.setCampania(camp);

        Asignacion a = new Asignacion();
        a.setUsuario(u);
        a.setProspecto(p);

        Contacto c = new Contacto();
        c.setAsignacion(a);
        c.setFechaContacto(LocalDateTime.now());
        c.setEstadoResultado(ResultadoAtencion.valueOf(resultado));
        return c;
    }
}
