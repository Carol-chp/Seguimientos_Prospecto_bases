package com.pe.swcotoschero.prospectos.Service;

import com.pe.swcotoschero.prospectos.Entity.Rol;
import com.pe.swcotoschero.prospectos.Entity.Usuario;
import com.pe.swcotoschero.prospectos.Repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

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
}
