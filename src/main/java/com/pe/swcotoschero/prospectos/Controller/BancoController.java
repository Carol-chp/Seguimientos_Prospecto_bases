package com.pe.swcotoschero.prospectos.Controller;

import com.pe.swcotoschero.prospectos.Service.BancoService;
import com.pe.swcotoschero.prospectos.dto.BancoRequestDTO;
import com.pe.swcotoschero.prospectos.dto.BancoResponseDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Catálogo de Bancos.
 *
 * GET /api/bancos          — lista bancos activos (cualquier autenticado; usado en selects)
 * GET /api/bancos/{id}     — detalle de un banco (ADMINISTRADOR)
 * POST /api/bancos         — crear banco (ADMINISTRADOR)
 * PUT  /api/bancos/{id}    — actualizar banco (ADMINISTRADOR)
 *
 * No se expone DELETE: la baja lógica se hace con activo=false vía PUT.
 * El GlobalExceptionHandler convierte IllegalArgumentException → 400 con mensaje legible.
 */
@RestController
@RequestMapping("/api/bancos")
@RequiredArgsConstructor
@Slf4j
public class BancoController {

    private final BancoService bancoService;

    /**
     * Lista de bancos activos ordenada por nombre.
     * Accesible a cualquier usuario autenticado (sin restricción de rol)
     * porque el frontend la usa en selects de asignación, creación de usuario y wizard.
     */
    @GetMapping
    public ResponseEntity<List<BancoResponseDTO>> listarActivos() {
        List<BancoResponseDTO> bancos = bancoService.listarActivos();
        return ResponseEntity.ok(bancos);
    }

    /**
     * Detalle de un banco por id.
     * Restringido a ADMINISTRADOR — el colaborador no necesita consultar bancos individuales.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<BancoResponseDTO> obtenerPorId(@PathVariable Long id) {
        return bancoService.obtenerPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Crear un nuevo banco.
     *
     * Request body:
     * {
     *   "nombre": "Interbank",           // obligatorio, único
     *   "bancoDestinoId": 2              // opcional — banco al que se reenvían OBSERVADO
     * }
     *
     * Response 201:
     * { "id": 3, "nombre": "Interbank", "activo": true, "esDefault": false,
     *   "bancoDestinoId": 2, "bancoDestinoNombre": "BBVA" }
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<BancoResponseDTO> crear(@Valid @RequestBody BancoRequestDTO request) {
        log.info("Crear banco: nombre='{}'", request.getNombre());
        BancoResponseDTO creado = bancoService.crear(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    /**
     * Actualizar un banco existente.
     *
     * Request body:
     * {
     *   "nombre": "Scotiabank Perú",     // obligatorio, debe ser único (ignora el propio)
     *   "activo": true,                  // opcional
     *   "esDefault": true,               // opcional — si true, desmarca el anterior default
     *   "bancoDestinoId": 2              // opcional — null limpia la relación
     * }
     *
     * Restricciones:
     * - bancoDestinoId no puede ser igual al propio id (auto-referencia).
     * - Solo puede haber un banco con esDefault=true.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<BancoResponseDTO> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody BancoRequestDTO request) {
        log.info("Actualizar banco id={}: nombre='{}'", id, request.getNombre());
        BancoResponseDTO actualizado = bancoService.actualizar(id, request);
        return ResponseEntity.ok(actualizado);
    }
}
