package com.pe.swcotoschero.prospectos.Service;

import com.pe.swcotoschero.prospectos.Entity.Rol;
import com.pe.swcotoschero.prospectos.Repository.RolRepository;
import com.pe.swcotoschero.prospectos.dto.RolDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RolServiceTest {

    @Mock
    private RolRepository rolRepository;

    @InjectMocks
    private RolService rolService;

    private List<Rol> testRoles;

    @BeforeEach
    void setUp() {
        Rol adminRol = new Rol();
        adminRol.setId(1L);
        adminRol.setNombre("ADMINISTRADOR");

        Rol teleoperadorRol = new Rol();
        teleoperadorRol.setId(2L);
        teleoperadorRol.setNombre("TELEOPERADOR");

        testRoles = Arrays.asList(adminRol, teleoperadorRol);
    }

    @Test
    void obtenerTodosLosRoles_Success() {
        // Arrange
        when(rolRepository.findAll()).thenReturn(testRoles);

        // Act
        List<RolDTO> result = rolService.obtenerTodosLosRoles();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());

        RolDTO adminDTO = result.get(0);
        assertEquals(1L, adminDTO.getId());
        assertEquals("ADMINISTRADOR", adminDTO.getNombre());

        RolDTO teleoperadorDTO = result.get(1);
        assertEquals(2L, teleoperadorDTO.getId());
        assertEquals("TELEOPERADOR", teleoperadorDTO.getNombre());

        verify(rolRepository).findAll();
    }

    @Test
    void obtenerTodosLosRoles_EmptyList() {
        // Arrange
        when(rolRepository.findAll()).thenReturn(Collections.emptyList());

        // Act
        List<RolDTO> result = rolService.obtenerTodosLosRoles();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(rolRepository).findAll();
    }

    @Test
    void obtenerTodosLosRoles_ThrowsRuntimeExceptionOnError() {
        // Arrange
        when(rolRepository.findAll()).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> rolService.obtenerTodosLosRoles()
        );

        assertEquals("Error al obtener los roles disponibles", exception.getMessage());
        verify(rolRepository).findAll();
    }

    @Test
    void obtenerTodosLosRoles_ConvertsCorrectly() {
        // Arrange
        Rol singleRol = new Rol();
        singleRol.setId(999L);
        singleRol.setNombre("TEST_ROL");
        when(rolRepository.findAll()).thenReturn(Collections.singletonList(singleRol));

        // Act
        List<RolDTO> result = rolService.obtenerTodosLosRoles();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        RolDTO dto = result.get(0);
        assertEquals(999L, dto.getId());
        assertEquals("TEST_ROL", dto.getNombre());
    }
}
