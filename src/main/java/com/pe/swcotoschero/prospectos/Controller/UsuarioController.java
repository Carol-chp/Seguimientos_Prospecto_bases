package com.pe.swcotoschero.prospectos.Controller;

import com.pe.swcotoschero.prospectos.Service.RolService;
import com.pe.swcotoschero.prospectos.Service.UsuarioService;
import com.pe.swcotoschero.prospectos.dto.CreateUsuarioRequestDTO;
import com.pe.swcotoschero.prospectos.dto.UpdateUsuarioRequestDTO;
import com.pe.swcotoschero.prospectos.dto.RolDTO;
import com.pe.swcotoschero.prospectos.dto.UsuarioDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
@Slf4j
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final RolService rolService;

    /**
     * Obtener todos los usuarios activos que no sean administradores
     * Para ser usados en asignaciones de prospectos
     */
    @GetMapping("/no-admins")
    public ResponseEntity<List<UsuarioDTO>> obtenerUsuariosNoAdministradores() {
        try {
            List<UsuarioDTO> usuarios = usuarioService.obtenerUsuariosNoAdministradores();
            return ResponseEntity.ok(usuarios);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Obtener usuarios por rol específico
     */
    @GetMapping("/por-rol/{rolId}")
    public ResponseEntity<List<UsuarioDTO>> obtenerUsuariosPorRol(@PathVariable Long rolId) {
        try {
            List<UsuarioDTO> usuarios = usuarioService.obtenerUsuariosPorRol(rolId);
            return ResponseEntity.ok(usuarios);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Obtener todos los usuarios activos
     */
    @GetMapping("/activos")
    public ResponseEntity<List<UsuarioDTO>> obtenerTodosLosUsuariosActivos() {
        try {
            List<UsuarioDTO> usuarios = usuarioService.obtenerTodosLosUsuariosActivos();
            return ResponseEntity.ok(usuarios);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Obtener usuario por ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<UsuarioDTO> obtenerUsuarioPorId(@PathVariable Long id) {
        try {
            Optional<UsuarioDTO> usuario = usuarioService.obtenerUsuarioPorId(id);
            return usuario.map(ResponseEntity::ok)
                          .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Obtener todos los roles disponibles
     */
    @GetMapping("/roles")
    public ResponseEntity<?> obtenerRoles() {
        try {
            log.info("Solicitud para obtener todos los roles");
            List<RolDTO> roles = rolService.obtenerTodosLosRoles();
            return ResponseEntity.ok(roles);
        } catch (Exception e) {
            log.error("Error al obtener roles: {}", e.getMessage(), e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Error al obtener los roles disponibles");
            error.put("code", "INTERNAL_ERROR");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Crear un nuevo usuario
     */
    @PostMapping
    public ResponseEntity<?> crearUsuario(@Valid @RequestBody CreateUsuarioRequestDTO request) {
        try {
            log.info("Solicitud para crear usuario: {}", request.getUsuario());
            UsuarioDTO usuarioCreado = usuarioService.crearUsuario(request);
            log.info("Usuario creado exitosamente con ID: {}", usuarioCreado.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(usuarioCreado);
        } catch (IllegalArgumentException e) {
            log.warn("Error de validación al crear usuario: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            error.put("code", "VALIDATION_ERROR");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
            log.error("Error inesperado al crear usuario: {}", e.getMessage(), e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Error al procesar la solicitud");
            error.put("code", "INTERNAL_ERROR");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Actualizar un usuario existente
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizarUsuario(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUsuarioRequestDTO request) {
        try {
            log.info("Solicitud para actualizar usuario con ID: {}", id);
            UsuarioDTO usuarioActualizado = usuarioService.actualizarUsuario(id, request);
            log.info("Usuario actualizado exitosamente con ID: {}", usuarioActualizado.getId());
            return ResponseEntity.ok(usuarioActualizado);
        } catch (IllegalArgumentException e) {
            log.warn("Error de validación al actualizar usuario con ID {}: {}", id, e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            error.put("code", "VALIDATION_ERROR");

            // Si el usuario no existe, retornar 404, de lo contrario 400
            if (e.getMessage().contains("no encontrado")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
            log.error("Error inesperado al actualizar usuario con ID {}: {}", id, e.getMessage(), e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Error al procesar la solicitud");
            error.put("code", "INTERNAL_ERROR");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Desactivar usuario (soft delete)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> desactivarUsuario(@PathVariable Long id) {
        try {
            log.info("Solicitud para desactivar usuario con ID: {}", id);
            UsuarioDTO usuarioDesactivado = usuarioService.desactivarUsuario(id);
            log.info("Usuario desactivado exitosamente con ID: {}", usuarioDesactivado.getId());
            return ResponseEntity.ok(usuarioDesactivado);
        } catch (IllegalArgumentException e) {
            log.warn("Error al desactivar usuario con ID {}: {}", id, e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            error.put("code", "VALIDATION_ERROR");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        } catch (Exception e) {
            log.error("Error inesperado al desactivar usuario con ID {}: {}", id, e.getMessage(), e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Error al procesar la solicitud");
            error.put("code", "INTERNAL_ERROR");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Manejo global de errores de validación
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(
            MethodArgumentNotValidException ex) {
        Map<String, Object> response = new HashMap<>();
        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        response.put("code", "VALIDATION_ERROR");
        response.put("errors", errors);

        log.warn("Errores de validación: {}", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
}