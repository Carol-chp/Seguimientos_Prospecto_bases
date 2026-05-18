package com.pe.swcotoschero.prospectos.Service;

import com.pe.swcotoschero.prospectos.Entity.Jornada;
import com.pe.swcotoschero.prospectos.Entity.Usuario;
import com.pe.swcotoschero.prospectos.Repository.JornadaRepository;
import com.pe.swcotoschero.prospectos.Repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/** RF-21 — jornada laboral del colaborador (iniciar / finalizar / estado). */
@Service
public class JornadaService {

    private final JornadaRepository jornadaRepository;
    private final UsuarioRepository usuarioRepository;

    public JornadaService(JornadaRepository jornadaRepository,
                          UsuarioRepository usuarioRepository) {
        this.jornadaRepository = jornadaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    private Usuario usuario(String username) {
        return usuarioRepository.findByUsuarioAndEstado(username, true)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
    }

    /** Marca inicio de jornada de hoy. Idempotente (no pisa un inicio ya hecho). */
    @Transactional
    public Map<String, Object> iniciar(String username) {
        Usuario u = usuario(username);
        LocalDate hoy = LocalDate.now();
        Jornada j = jornadaRepository.findByUsuario_IdAndFecha(u.getId(), hoy)
                .orElseGet(() -> {
                    Jornada nueva = new Jornada();
                    nueva.setUsuario(u);
                    nueva.setFecha(hoy);
                    return nueva;
                });
        if (j.getInicio() == null) {
            j.setInicio(LocalDateTime.now());
            jornadaRepository.save(j);
        }
        return estado(j);
    }

    /** Marca fin de jornada. Requiere haber iniciado primero. */
    @Transactional
    public Map<String, Object> finalizar(String username) {
        Usuario u = usuario(username);
        Jornada j = jornadaRepository
                .findByUsuario_IdAndFecha(u.getId(), LocalDate.now())
                .orElseThrow(() -> new IllegalArgumentException(
                        "No hay jornada iniciada hoy para finalizar."));
        if (j.getInicio() == null) {
            throw new IllegalArgumentException("Debe iniciar la jornada antes de finalizarla.");
        }
        if (j.getFin() == null) {
            j.setFin(LocalDateTime.now());
            jornadaRepository.save(j);
        }
        return estado(j);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> estadoHoy(String username) {
        Usuario u = usuario(username);
        return jornadaRepository.findByUsuario_IdAndFecha(u.getId(), LocalDate.now())
                .map(this::estado)
                .orElseGet(() -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("fecha", LocalDate.now().toString());
                    m.put("iniciada", false);
                    m.put("finalizada", false);
                    m.put("inicio", null);
                    m.put("fin", null);
                    return m;
                });
    }

    private Map<String, Object> estado(Jornada j) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("fecha", j.getFecha() != null ? j.getFecha().toString() : null);
        m.put("iniciada", j.getInicio() != null);
        m.put("finalizada", j.getFin() != null);
        m.put("inicio", j.getInicio() != null ? j.getInicio().toString() : null);
        m.put("fin", j.getFin() != null ? j.getFin().toString() : null);
        return m;
    }
}
