package com.pe.swcotoschero.prospectos.Controller;

import com.pe.swcotoschero.prospectos.Service.UsuarioService;
import com.pe.swcotoschero.prospectos.dto.UsuarioDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

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
}