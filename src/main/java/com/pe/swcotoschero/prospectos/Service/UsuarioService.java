package com.pe.swcotoschero.prospectos.Service;

import com.pe.swcotoschero.prospectos.Entity.Rol;
import com.pe.swcotoschero.prospectos.Entity.Usuario;
import com.pe.swcotoschero.prospectos.Repository.UsuarioRepository;
import com.pe.swcotoschero.prospectos.dto.UsuarioDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
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
