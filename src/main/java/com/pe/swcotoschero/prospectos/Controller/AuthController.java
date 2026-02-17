package com.pe.swcotoschero.prospectos.Controller;

import com.pe.swcotoschero.prospectos.Entity.Usuario;
import com.pe.swcotoschero.prospectos.Repository.UsuarioRepository;
import com.pe.swcotoschero.prospectos.Service.JwtService;
import com.pe.swcotoschero.prospectos.Service.UsuarioService;
import com.pe.swcotoschero.prospectos.dto.AuthRequest;
import com.pe.swcotoschero.prospectos.dto.LoginResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UsuarioService usuarioService;
    private final UsuarioRepository usuarioRepository;

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;



    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request) throws InterruptedException {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String jwt = jwtService.generateToken(userDetails);

        Usuario usuario = usuarioRepository.findByUsuarioAndEstado(userDetails.getUsername(), true)
                .orElse(null);

        String rol = usuario != null ? usuario.getRol().getNombre() : "";
        String nombre = usuario != null ? usuario.getNombre() + " " + usuario.getApellidos() : "";

        return ResponseEntity.ok(LoginResponseDTO.builder().token(jwt).rol(rol).nombre(nombre).build());
    }

    @PostMapping("/register")
    public ResponseEntity<String> register() {

        usuarioService.register();
        return ResponseEntity.ok("Usuario registrado exitosamente");
    }
}
