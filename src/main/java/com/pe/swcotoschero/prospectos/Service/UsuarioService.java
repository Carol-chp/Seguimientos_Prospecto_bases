package com.pe.swcotoschero.prospectos.Service;

import com.pe.swcotoschero.prospectos.Entity.Rol;
import com.pe.swcotoschero.prospectos.Entity.Usuario;
import com.pe.swcotoschero.prospectos.Repository.RolRepository;
import com.pe.swcotoschero.prospectos.Repository.UsuarioRepository;
import com.pe.swcotoschero.prospectos.dto.CreateUsuarioRequestDTO;
import com.pe.swcotoschero.prospectos.dto.UpdateUsuarioRequestDTO;
import com.pe.swcotoschero.prospectos.dto.UsuarioDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    
    // ID del rol de administrador (según datos de la BD)
    private static final Long ADMIN_ROLE_ID = 1L;

    public void register() {
        Usuario usuario = new Usuario();
        usuario.setUsuario("JCOTOS");
        usuario.setPassword(passwordEncoder.encode("123456"));
        usuario.setNombre("Johnny");
        usuario.setApellidos("Cotos");
        usuario.setEmail("jrcotos@gmail.com");
        usuario.setEstado(true);
        usuario.setRol(new Rol());
        usuario.getRol().setId(1L);
        usuarioRepository.save(usuario);
    }

    /**
     * Crear un nuevo usuario
     * Valida que el username no exista y encripta la contraseña
     *
     * @param request DTO con los datos del nuevo usuario
     * @return UsuarioDTO del usuario creado
     * @throws IllegalArgumentException si el usuario ya existe o el rol no existe
     */
    @Transactional
    public UsuarioDTO crearUsuario(CreateUsuarioRequestDTO request) {
        try {
            log.info("Iniciando creación de usuario con username: {}", request.getUsuario());

            // Validar que el username no exista ya
            Optional<Usuario> usuarioExistente = usuarioRepository.findByUsuarioAndEstado(
                request.getUsuario(), true
            );

            if (usuarioExistente.isPresent()) {
                log.warn("Intento de crear usuario duplicado: {}", request.getUsuario());
                throw new IllegalArgumentException("El nombre de usuario ya está en uso");
            }

            // Validar que el rol exista
            Rol rol = rolRepository.findById(request.getRolId())
                .orElseThrow(() -> {
                    log.error("Rol no encontrado con ID: {}", request.getRolId());
                    return new IllegalArgumentException("El rol especificado no existe");
                });

            // Crear el nuevo usuario
            Usuario nuevoUsuario = new Usuario();
            nuevoUsuario.setNombre(request.getNombre());
            nuevoUsuario.setApellidos(request.getApellidos());
            nuevoUsuario.setUsuario(request.getUsuario());
            nuevoUsuario.setEmail(request.getEmail());
            nuevoUsuario.setPassword(passwordEncoder.encode(request.getPassword()));
            nuevoUsuario.setEstado(true);
            nuevoUsuario.setRol(rol);

            // Guardar el usuario
            Usuario usuarioGuardado = usuarioRepository.save(nuevoUsuario);
            log.info("Usuario creado exitosamente con ID: {} y username: {}",
                usuarioGuardado.getId(), usuarioGuardado.getUsuario());

            // Convertir a DTO y retornar
            return convertirADTO(usuarioGuardado);

        } catch (IllegalArgumentException e) {
            // Re-lanzar excepciones de validación
            throw e;
        } catch (Exception e) {
            log.error("Error al crear usuario: {}", e.getMessage(), e);
            throw new RuntimeException("Error al crear el usuario. Por favor, intente nuevamente.");
        }
    }
    
    /**
     * Obtener todos los usuarios activos que no sean administradores
     */
    public List<UsuarioDTO> obtenerUsuariosNoAdministradores() {
        List<Usuario> usuarios = usuarioRepository.findActiveUsersWithoutAdminRole(ADMIN_ROLE_ID);
        return usuarios.stream()
            .map(this::convertirADTO)
            .toList();
    }
    
    /**
     * Obtener usuarios por rol
     */
    public List<UsuarioDTO> obtenerUsuariosPorRol(Long rolId) {
        List<Usuario> usuarios = usuarioRepository.findByRol_IdAndEstadoOrderByNombreAsc(rolId, true);
        return usuarios.stream()
            .map(this::convertirADTO)
            .toList();
    }
    
    /**
     * Obtener todos los usuarios activos
     */
    public List<UsuarioDTO> obtenerTodosLosUsuariosActivos() {
        List<Usuario> usuarios = usuarioRepository.findAll().stream()
            .filter(u -> u.getEstado() != null && u.getEstado())
            .toList();
        return usuarios.stream()
            .map(this::convertirADTO)
            .toList();
    }
    
    /**
     * Obtener usuario por ID
     */
    public Optional<UsuarioDTO> obtenerUsuarioPorId(Long id) {
        return usuarioRepository.findById(id)
            .map(this::convertirADTO);
    }

    /**
     * Actualizar un usuario existente
     * El username NO es editable. El password es opcional (solo se actualiza si se proporciona).
     *
     * @param id ID del usuario a actualizar
     * @param request DTO con los datos a actualizar
     * @return UsuarioDTO del usuario actualizado
     * @throws IllegalArgumentException si el usuario no existe o el rol no existe
     */
    @Transactional
    public UsuarioDTO actualizarUsuario(Long id, UpdateUsuarioRequestDTO request) {
        try {
            log.info("Iniciando actualización del usuario con ID: {}", id);

            // Validar que el usuario exista
            Usuario usuarioExistente = usuarioRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Usuario no encontrado con ID: {}", id);
                    return new IllegalArgumentException("Usuario no encontrado");
                });

            // Validar que el rol exista
            Rol rol = rolRepository.findById(request.getRolId())
                .orElseThrow(() -> {
                    log.error("Rol no encontrado con ID: {}", request.getRolId());
                    return new IllegalArgumentException("El rol especificado no existe");
                });

            // Actualizar campos del usuario
            usuarioExistente.setNombre(request.getNombre());
            usuarioExistente.setApellidos(request.getApellidos());
            usuarioExistente.setEmail(request.getEmail());
            usuarioExistente.setEstado(request.getEstado());
            usuarioExistente.setRol(rol);

            // Solo actualizar password si se proporciona (no null y no vacío)
            if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
                log.info("Actualizando password para usuario ID: {}", id);
                usuarioExistente.setPassword(passwordEncoder.encode(request.getPassword()));
            }

            // Guardar cambios
            Usuario usuarioActualizado = usuarioRepository.save(usuarioExistente);
            log.info("Usuario actualizado exitosamente con ID: {} y username: {}",
                usuarioActualizado.getId(), usuarioActualizado.getUsuario());

            // Convertir a DTO y retornar
            return convertirADTO(usuarioActualizado);

        } catch (IllegalArgumentException e) {
            // Re-lanzar excepciones de validación
            throw e;
        } catch (Exception e) {
            log.error("Error al actualizar usuario con ID {}: {}", id, e.getMessage(), e);
            throw new RuntimeException("Error al actualizar el usuario. Por favor, intente nuevamente.");
        }
    }

    /**
     * Desactivar usuario (soft delete)
     * Cambia el estado del usuario a false en lugar de eliminarlo físicamente
     *
     * @param id ID del usuario a desactivar
     * @return UsuarioDTO del usuario desactivado
     * @throws IllegalArgumentException si el usuario no existe
     */
    @Transactional
    public UsuarioDTO desactivarUsuario(Long id) {
        try {
            log.info("Iniciando desactivación del usuario con ID: {}", id);

            // Validar que el usuario exista
            Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Usuario no encontrado con ID: {}", id);
                    return new IllegalArgumentException("Usuario no encontrado");
                });

            // Cambiar estado a false (soft delete)
            usuario.setEstado(false);

            // Guardar cambios
            Usuario usuarioDesactivado = usuarioRepository.save(usuario);
            log.info("Usuario desactivado exitosamente con ID: {} y username: {}",
                usuarioDesactivado.getId(), usuarioDesactivado.getUsuario());

            // Convertir a DTO y retornar
            return convertirADTO(usuarioDesactivado);

        } catch (IllegalArgumentException e) {
            // Re-lanzar excepciones de validación
            throw e;
        } catch (Exception e) {
            log.error("Error al desactivar usuario con ID {}: {}", id, e.getMessage(), e);
            throw new RuntimeException("Error al desactivar el usuario. Por favor, intente nuevamente.");
        }
    }

    /**
     * Convertir entidad Usuario a DTO
     */
    private UsuarioDTO convertirADTO(Usuario usuario) {
        UsuarioDTO dto = new UsuarioDTO();
        dto.setId(usuario.getId());
        dto.setNombre(usuario.getNombre());
        dto.setApellidos(usuario.getApellidos());
        dto.setUsuario(usuario.getUsuario());
        dto.setEmail(usuario.getEmail());
        dto.setEstado(usuario.getEstado());

        // Información del rol
        if (usuario.getRol() != null) {
            dto.setRolId(usuario.getRol().getId());
            dto.setRolNombre(usuario.getRol().getNombre());
        }

        return dto;
    }
}
