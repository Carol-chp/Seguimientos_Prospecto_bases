package com.pe.swcotoschero.prospectos.Controller;

import com.pe.swcotoschero.prospectos.Entity.TarjetaWhatsapp;
import com.pe.swcotoschero.prospectos.Entity.Usuario;
import com.pe.swcotoschero.prospectos.Repository.ConfiguracionDuenoRepository;
import com.pe.swcotoschero.prospectos.Repository.TarjetaWhatsappRepository;
import com.pe.swcotoschero.prospectos.Repository.UsuarioRepository;
import com.pe.swcotoschero.prospectos.dto.TarjetaWhatsappRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;

/**
 * WhatsApp del colaborador (RF nuevo): plantilla del mensaje y tarjeta/firma
 * por colaborador. El envío real lo hace el navegador vía enlace wa.me; aquí
 * solo se sirve el texto y la imagen.
 */
@RestController
@RequestMapping("/api/whatsapp")
public class WhatsappController {

    private static final long MAX_BYTES = 2 * 1024 * 1024; // 2 MB

    private final ConfiguracionDuenoRepository configRepo;
    private final TarjetaWhatsappRepository tarjetaRepo;
    private final UsuarioRepository usuarioRepository;

    public WhatsappController(ConfiguracionDuenoRepository configRepo,
                              TarjetaWhatsappRepository tarjetaRepo,
                              UsuarioRepository usuarioRepository) {
        this.configRepo = configRepo;
        this.tarjetaRepo = tarjetaRepo;
        this.usuarioRepository = usuarioRepository;
    }

    private Usuario autenticado() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return usuarioRepository.findByUsuarioAndEstado(username, true)
                .orElseThrow(() -> new IllegalArgumentException("Usuario autenticado no encontrado"));
    }

    /** Plantilla del mensaje (variables {nombre}, {asesor}). Cualquier autenticado. */
    @GetMapping("/plantilla")
    public ResponseEntity<Map<String, String>> plantilla() {
        String p = configRepo.findTopByOrderByIdAsc()
                .map(c -> c.getPlantillaWhatsapp())
                .orElse(null);
        return ResponseEntity.ok(Map.of("plantilla", p == null ? "" : p));
    }

    /** Tarjeta/firma del colaborador autenticado (imagen). 404 si no tiene. */
    @GetMapping("/mi-tarjeta")
    public ResponseEntity<byte[]> miTarjeta() {
        Usuario u = autenticado();
        return tarjetaRepo.findById(u.getId())
                .map(t -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(t.getTipo()))
                        .body(t.getImagen()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /** ¿El colaborador autenticado tiene tarjeta cargada? (para mostrar/ocultar el botón). */
    @GetMapping("/mi-tarjeta/existe")
    public ResponseEntity<Map<String, Boolean>> existeMiTarjeta() {
        return ResponseEntity.ok(Map.of("existe",
                tarjetaRepo.existsById(autenticado().getId())));
    }

    /** Sube/reemplaza la tarjeta de un colaborador. Solo ADMINISTRADOR. */
    @PostMapping("/usuario/{id}/tarjeta")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Map<String, Object>> subirTarjeta(
            @PathVariable Long id, @RequestBody TarjetaWhatsappRequest req) {
        if (!usuarioRepository.existsById(id)) {
            throw new IllegalArgumentException("Colaborador no encontrado: " + id);
        }
        if (req == null || req.getBase64() == null || req.getBase64().isBlank()) {
            throw new IllegalArgumentException("La imagen es obligatoria.");
        }
        String tipo = req.getContentType();
        if (tipo == null || !tipo.toLowerCase().startsWith("image/")) {
            throw new IllegalArgumentException("El archivo debe ser una imagen (image/*).");
        }
        final byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(req.getBase64().trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("La imagen no es Base64 válido.");
        }
        if (bytes.length == 0) {
            throw new IllegalArgumentException("La imagen está vacía.");
        }
        if (bytes.length > MAX_BYTES) {
            throw new IllegalArgumentException("La imagen excede el máximo de 2 MB.");
        }
        TarjetaWhatsapp t = tarjetaRepo.findById(id).orElseGet(TarjetaWhatsapp::new);
        t.setUsuarioId(id);
        t.setImagen(bytes);
        t.setTipo(tipo);
        t.setActualizado(LocalDateTime.now());
        tarjetaRepo.save(t);
        return ResponseEntity.ok(Map.of("ok", true, "bytes", bytes.length));
    }
}
