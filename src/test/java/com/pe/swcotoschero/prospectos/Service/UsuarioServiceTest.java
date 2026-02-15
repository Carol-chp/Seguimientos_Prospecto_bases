package com.pe.swcotoschero.prospectos.Service;

import com.pe.swcotoschero.prospectos.Entity.Rol;
import com.pe.swcotoschero.prospectos.Entity.Usuario;
import com.pe.swcotoschero.prospectos.Repository.RolRepository;
import com.pe.swcotoschero.prospectos.Repository.UsuarioRepository;
import com.pe.swcotoschero.prospectos.dto.CreateUsuarioRequestDTO;
import com.pe.swcotoschero.prospectos.dto.UpdateUsuarioRequestDTO;
import com.pe.swcotoschero.prospectos.dto.UsuarioDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private RolRepository rolRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UsuarioService usuarioService;

    private CreateUsuarioRequestDTO validRequest;
    private Rol testRol;
    private Usuario testUsuario;

    @BeforeEach
    void setUp() {
        // Configurar datos de prueba
        validRequest = new CreateUsuarioRequestDTO(
            "Juan",
            "Pérez",
            "jperez",
            "jperez@example.com",
            "password123",
            2L
        );

        testRol = new Rol();
        testRol.setId(2L);
        testRol.setNombre("TELEOPERADOR");

        testUsuario = new Usuario();
        testUsuario.setId(1L);
        testUsuario.setNombre("Juan");
        testUsuario.setApellidos("Pérez");
        testUsuario.setUsuario("jperez");
        testUsuario.setEmail("jperez@example.com");
        testUsuario.setPassword("encodedPassword");
        testUsuario.setEstado(true);
        testUsuario.setRol(testRol);
    }

    @Test
    void crearUsuario_Success() {
        // Arrange
        when(usuarioRepository.findByUsuarioAndEstado("jperez", true))
            .thenReturn(Optional.empty());
        when(rolRepository.findById(2L))
            .thenReturn(Optional.of(testRol));
        when(passwordEncoder.encode("password123"))
            .thenReturn("encodedPassword");
        when(usuarioRepository.save(any(Usuario.class)))
            .thenReturn(testUsuario);

        // Act
        UsuarioDTO result = usuarioService.crearUsuario(validRequest);

        // Assert
        assertNotNull(result);
        assertEquals("Juan", result.getNombre());
        assertEquals("Pérez", result.getApellidos());
        assertEquals("jperez", result.getUsuario());
        assertEquals("jperez@example.com", result.getEmail());
        assertEquals(true, result.getEstado());
        assertEquals(2L, result.getRolId());
        assertEquals("TELEOPERADOR", result.getRolNombre());

        // Verify interactions
        verify(usuarioRepository).findByUsuarioAndEstado("jperez", true);
        verify(rolRepository).findById(2L);
        verify(passwordEncoder).encode("password123");
        verify(usuarioRepository).save(any(Usuario.class));
    }

    @Test
    void crearUsuario_PasswordEncrypted() {
        // Arrange
        when(usuarioRepository.findByUsuarioAndEstado(anyString(), anyBoolean()))
            .thenReturn(Optional.empty());
        when(rolRepository.findById(anyLong()))
            .thenReturn(Optional.of(testRol));
        when(passwordEncoder.encode(anyString()))
            .thenReturn("encodedPassword");
        when(usuarioRepository.save(any(Usuario.class)))
            .thenReturn(testUsuario);

        ArgumentCaptor<Usuario> usuarioCaptor = ArgumentCaptor.forClass(Usuario.class);

        // Act
        usuarioService.crearUsuario(validRequest);

        // Assert
        verify(usuarioRepository).save(usuarioCaptor.capture());
        Usuario savedUsuario = usuarioCaptor.getValue();
        assertEquals("encodedPassword", savedUsuario.getPassword());
        assertNotEquals("password123", savedUsuario.getPassword());
    }

    @Test
    void crearUsuario_EstadoSetToTrue() {
        // Arrange
        when(usuarioRepository.findByUsuarioAndEstado(anyString(), anyBoolean()))
            .thenReturn(Optional.empty());
        when(rolRepository.findById(anyLong()))
            .thenReturn(Optional.of(testRol));
        when(passwordEncoder.encode(anyString()))
            .thenReturn("encodedPassword");
        when(usuarioRepository.save(any(Usuario.class)))
            .thenReturn(testUsuario);

        ArgumentCaptor<Usuario> usuarioCaptor = ArgumentCaptor.forClass(Usuario.class);

        // Act
        usuarioService.crearUsuario(validRequest);

        // Assert
        verify(usuarioRepository).save(usuarioCaptor.capture());
        Usuario savedUsuario = usuarioCaptor.getValue();
        assertTrue(savedUsuario.getEstado());
    }

    @Test
    void crearUsuario_ThrowsExceptionWhenUsernameExists() {
        // Arrange
        when(usuarioRepository.findByUsuarioAndEstado("jperez", true))
            .thenReturn(Optional.of(testUsuario));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> usuarioService.crearUsuario(validRequest)
        );

        assertEquals("El nombre de usuario ya está en uso", exception.getMessage());
        verify(usuarioRepository).findByUsuarioAndEstado("jperez", true);
        verify(rolRepository, never()).findById(anyLong());
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void crearUsuario_ThrowsExceptionWhenRolNotFound() {
        // Arrange
        when(usuarioRepository.findByUsuarioAndEstado(anyString(), anyBoolean()))
            .thenReturn(Optional.empty());
        when(rolRepository.findById(2L))
            .thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> usuarioService.crearUsuario(validRequest)
        );

        assertEquals("El rol especificado no existe", exception.getMessage());
        verify(usuarioRepository).findByUsuarioAndEstado("jperez", true);
        verify(rolRepository).findById(2L);
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void crearUsuario_HandlesRepositoryException() {
        // Arrange
        when(usuarioRepository.findByUsuarioAndEstado(anyString(), anyBoolean()))
            .thenReturn(Optional.empty());
        when(rolRepository.findById(anyLong()))
            .thenReturn(Optional.of(testRol));
        when(passwordEncoder.encode(anyString()))
            .thenReturn("encodedPassword");
        when(usuarioRepository.save(any(Usuario.class)))
            .thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> usuarioService.crearUsuario(validRequest)
        );

        assertEquals("Error al crear el usuario. Por favor, intente nuevamente.",
            exception.getMessage());
    }

    @Test
    void crearUsuario_AllFieldsSetCorrectly() {
        // Arrange
        when(usuarioRepository.findByUsuarioAndEstado(anyString(), anyBoolean()))
            .thenReturn(Optional.empty());
        when(rolRepository.findById(anyLong()))
            .thenReturn(Optional.of(testRol));
        when(passwordEncoder.encode(anyString()))
            .thenReturn("encodedPassword");
        when(usuarioRepository.save(any(Usuario.class)))
            .thenReturn(testUsuario);

        ArgumentCaptor<Usuario> usuarioCaptor = ArgumentCaptor.forClass(Usuario.class);

        // Act
        usuarioService.crearUsuario(validRequest);

        // Assert
        verify(usuarioRepository).save(usuarioCaptor.capture());
        Usuario savedUsuario = usuarioCaptor.getValue();

        assertEquals("Juan", savedUsuario.getNombre());
        assertEquals("Pérez", savedUsuario.getApellidos());
        assertEquals("jperez", savedUsuario.getUsuario());
        assertEquals("jperez@example.com", savedUsuario.getEmail());
        assertEquals("encodedPassword", savedUsuario.getPassword());
        assertTrue(savedUsuario.getEstado());
        assertNotNull(savedUsuario.getRol());
        assertEquals(2L, savedUsuario.getRol().getId());
    }

    // ============================================
    // Tests para actualizarUsuario
    // ============================================

    @Test
    void actualizarUsuario_Success() {
        // Arrange
        UpdateUsuarioRequestDTO updateRequest = new UpdateUsuarioRequestDTO(
            "Carlos",
            "García",
            "cgarcia@example.com",
            null, // Sin cambio de password
            2L,
            true
        );

        when(usuarioRepository.findById(1L))
            .thenReturn(Optional.of(testUsuario));
        when(rolRepository.findById(2L))
            .thenReturn(Optional.of(testRol));
        when(usuarioRepository.save(any(Usuario.class)))
            .thenReturn(testUsuario);

        // Act
        UsuarioDTO result = usuarioService.actualizarUsuario(1L, updateRequest);

        // Assert
        assertNotNull(result);
        verify(usuarioRepository).findById(1L);
        verify(rolRepository).findById(2L);
        verify(usuarioRepository).save(any(Usuario.class));
        verify(passwordEncoder, never()).encode(anyString()); // No se debe encriptar si no hay password
    }

    @Test
    void actualizarUsuario_WithPassword() {
        // Arrange
        UpdateUsuarioRequestDTO updateRequest = new UpdateUsuarioRequestDTO(
            "Carlos",
            "García",
            "cgarcia@example.com",
            "newPassword123",
            2L,
            true
        );

        when(usuarioRepository.findById(1L))
            .thenReturn(Optional.of(testUsuario));
        when(rolRepository.findById(2L))
            .thenReturn(Optional.of(testRol));
        when(passwordEncoder.encode("newPassword123"))
            .thenReturn("newEncodedPassword");
        when(usuarioRepository.save(any(Usuario.class)))
            .thenReturn(testUsuario);

        ArgumentCaptor<Usuario> usuarioCaptor = ArgumentCaptor.forClass(Usuario.class);

        // Act
        usuarioService.actualizarUsuario(1L, updateRequest);

        // Assert
        verify(usuarioRepository).save(usuarioCaptor.capture());
        Usuario savedUsuario = usuarioCaptor.getValue();
        assertEquals("newEncodedPassword", savedUsuario.getPassword());
        verify(passwordEncoder).encode("newPassword123");
    }

    @Test
    void actualizarUsuario_WithEmptyPassword_DoesNotUpdatePassword() {
        // Arrange
        UpdateUsuarioRequestDTO updateRequest = new UpdateUsuarioRequestDTO(
            "Carlos",
            "García",
            "cgarcia@example.com",
            "   ", // Password vacío
            2L,
            true
        );

        String originalPassword = testUsuario.getPassword();

        when(usuarioRepository.findById(1L))
            .thenReturn(Optional.of(testUsuario));
        when(rolRepository.findById(2L))
            .thenReturn(Optional.of(testRol));
        when(usuarioRepository.save(any(Usuario.class)))
            .thenReturn(testUsuario);

        ArgumentCaptor<Usuario> usuarioCaptor = ArgumentCaptor.forClass(Usuario.class);

        // Act
        usuarioService.actualizarUsuario(1L, updateRequest);

        // Assert
        verify(usuarioRepository).save(usuarioCaptor.capture());
        Usuario savedUsuario = usuarioCaptor.getValue();
        assertEquals(originalPassword, savedUsuario.getPassword()); // Password no cambia
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void actualizarUsuario_AllFieldsUpdatedCorrectly() {
        // Arrange
        UpdateUsuarioRequestDTO updateRequest = new UpdateUsuarioRequestDTO(
            "Carlos",
            "García",
            "cgarcia@example.com",
            null,
            2L,
            false
        );

        when(usuarioRepository.findById(1L))
            .thenReturn(Optional.of(testUsuario));
        when(rolRepository.findById(2L))
            .thenReturn(Optional.of(testRol));
        when(usuarioRepository.save(any(Usuario.class)))
            .thenReturn(testUsuario);

        ArgumentCaptor<Usuario> usuarioCaptor = ArgumentCaptor.forClass(Usuario.class);

        // Act
        usuarioService.actualizarUsuario(1L, updateRequest);

        // Assert
        verify(usuarioRepository).save(usuarioCaptor.capture());
        Usuario savedUsuario = usuarioCaptor.getValue();

        assertEquals("Carlos", savedUsuario.getNombre());
        assertEquals("García", savedUsuario.getApellidos());
        assertEquals("cgarcia@example.com", savedUsuario.getEmail());
        assertEquals(false, savedUsuario.getEstado());
        assertEquals(testRol, savedUsuario.getRol());
    }

    @Test
    void actualizarUsuario_ThrowsExceptionWhenUserNotFound() {
        // Arrange
        UpdateUsuarioRequestDTO updateRequest = new UpdateUsuarioRequestDTO(
            "Carlos",
            "García",
            "cgarcia@example.com",
            null,
            2L,
            true
        );

        when(usuarioRepository.findById(99L))
            .thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> usuarioService.actualizarUsuario(99L, updateRequest)
        );

        assertEquals("Usuario no encontrado", exception.getMessage());
        verify(usuarioRepository).findById(99L);
        verify(rolRepository, never()).findById(anyLong());
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void actualizarUsuario_ThrowsExceptionWhenRolNotFound() {
        // Arrange
        UpdateUsuarioRequestDTO updateRequest = new UpdateUsuarioRequestDTO(
            "Carlos",
            "García",
            "cgarcia@example.com",
            null,
            99L,
            true
        );

        when(usuarioRepository.findById(1L))
            .thenReturn(Optional.of(testUsuario));
        when(rolRepository.findById(99L))
            .thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> usuarioService.actualizarUsuario(1L, updateRequest)
        );

        assertEquals("El rol especificado no existe", exception.getMessage());
        verify(usuarioRepository).findById(1L);
        verify(rolRepository).findById(99L);
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void actualizarUsuario_HandlesRepositoryException() {
        // Arrange
        UpdateUsuarioRequestDTO updateRequest = new UpdateUsuarioRequestDTO(
            "Carlos",
            "García",
            "cgarcia@example.com",
            null,
            2L,
            true
        );

        when(usuarioRepository.findById(1L))
            .thenReturn(Optional.of(testUsuario));
        when(rolRepository.findById(2L))
            .thenReturn(Optional.of(testRol));
        when(usuarioRepository.save(any(Usuario.class)))
            .thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> usuarioService.actualizarUsuario(1L, updateRequest)
        );

        assertEquals("Error al actualizar el usuario. Por favor, intente nuevamente.",
            exception.getMessage());
    }

    // ============================================
    // Tests para desactivarUsuario
    // ============================================

    @Test
    void desactivarUsuario_Success() {
        // Arrange
        when(usuarioRepository.findById(1L))
            .thenReturn(Optional.of(testUsuario));
        when(usuarioRepository.save(any(Usuario.class)))
            .thenReturn(testUsuario);

        // Act
        UsuarioDTO result = usuarioService.desactivarUsuario(1L);

        // Assert
        assertNotNull(result);
        verify(usuarioRepository).findById(1L);
        verify(usuarioRepository).save(any(Usuario.class));
    }

    @Test
    void desactivarUsuario_SetsEstadoToFalse() {
        // Arrange
        when(usuarioRepository.findById(1L))
            .thenReturn(Optional.of(testUsuario));
        when(usuarioRepository.save(any(Usuario.class)))
            .thenReturn(testUsuario);

        ArgumentCaptor<Usuario> usuarioCaptor = ArgumentCaptor.forClass(Usuario.class);

        // Act
        usuarioService.desactivarUsuario(1L);

        // Assert
        verify(usuarioRepository).save(usuarioCaptor.capture());
        Usuario savedUsuario = usuarioCaptor.getValue();
        assertFalse(savedUsuario.getEstado());
    }

    @Test
    void desactivarUsuario_ThrowsExceptionWhenUserNotFound() {
        // Arrange
        when(usuarioRepository.findById(99L))
            .thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> usuarioService.desactivarUsuario(99L)
        );

        assertEquals("Usuario no encontrado", exception.getMessage());
        verify(usuarioRepository).findById(99L);
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void desactivarUsuario_HandlesRepositoryException() {
        // Arrange
        when(usuarioRepository.findById(1L))
            .thenReturn(Optional.of(testUsuario));
        when(usuarioRepository.save(any(Usuario.class)))
            .thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> usuarioService.desactivarUsuario(1L)
        );

        assertEquals("Error al desactivar el usuario. Por favor, intente nuevamente.",
            exception.getMessage());
    }

    @Test
    void desactivarUsuario_DoesNotModifyOtherFields() {
        // Arrange
        when(usuarioRepository.findById(1L))
            .thenReturn(Optional.of(testUsuario));
        when(usuarioRepository.save(any(Usuario.class)))
            .thenReturn(testUsuario);

        String originalNombre = testUsuario.getNombre();
        String originalApellidos = testUsuario.getApellidos();
        String originalEmail = testUsuario.getEmail();
        String originalUsuario = testUsuario.getUsuario();

        ArgumentCaptor<Usuario> usuarioCaptor = ArgumentCaptor.forClass(Usuario.class);

        // Act
        usuarioService.desactivarUsuario(1L);

        // Assert
        verify(usuarioRepository).save(usuarioCaptor.capture());
        Usuario savedUsuario = usuarioCaptor.getValue();

        // Verificar que solo el estado cambió
        assertFalse(savedUsuario.getEstado());
        assertEquals(originalNombre, savedUsuario.getNombre());
        assertEquals(originalApellidos, savedUsuario.getApellidos());
        assertEquals(originalEmail, savedUsuario.getEmail());
        assertEquals(originalUsuario, savedUsuario.getUsuario());
    }
}
