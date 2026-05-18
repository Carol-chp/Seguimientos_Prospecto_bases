package com.pe.swcotoschero.prospectos.Controller;

import com.pe.swcotoschero.prospectos.Entity.Usuario;
import com.pe.swcotoschero.prospectos.Repository.UsuarioRepository;
import com.pe.swcotoschero.prospectos.Service.JornadaService;
import com.pe.swcotoschero.prospectos.Service.JwtService;
import com.pe.swcotoschero.prospectos.dto.AuthRequest;
import com.pe.swcotoschero.prospectos.dto.LoginResponseDTO;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private static final String ROL_TELEOPERADOR = "TELEOPERADOR";

    private final UsuarioRepository usuarioRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final JornadaService jornadaService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(), request.getPassword()));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String jwt = jwtService.generateToken(userDetails);

        Usuario usuario = usuarioRepository
                .findByUsuarioAndEstado(userDetails.getUsername(), true)
                .orElse(null);

        String rol = usuario != null ? usuario.getRol().getNombre() : "";
        String nombre = usuario != null ? usuario.getNombre() + " " + usuario.getApellidos() : "";

        // Auto-inicio de jornada en el primer login del día (RF-21). iniciar() es
        // idempotente: solo registra el inicio si aún no existe hoy. Best-effort:
        // un fallo aquí NO debe impedir el login.
        if (ROL_TELEOPERADOR.equalsIgnoreCase(rol)) {
            try {
                jornadaService.iniciar(userDetails.getUsername());
            } catch (Exception e) {
                log.warn("No se pudo auto-iniciar jornada de {}: {}",
                        userDetails.getUsername(), e.getMessage());
            }
        }

        return ResponseEntity.ok(
                LoginResponseDTO.builder().token(jwt).rol(rol).nombre(nombre).build());
    }

    // POST /api/auth/register eliminado intencionalmente (Fase 0.3).
    // La creacion de usuarios se realiza a traves de /api/usuarios (rol ADMINISTRADOR).
}
