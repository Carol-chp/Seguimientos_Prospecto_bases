package com.pe.swcotoschero.prospectos.Service;

import com.pe.swcotoschero.prospectos.Entity.Banco;
import com.pe.swcotoschero.prospectos.Repository.BancoRepository;
import com.pe.swcotoschero.prospectos.dto.BancoRequestDTO;
import com.pe.swcotoschero.prospectos.dto.BancoResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class BancoService {

    private final BancoRepository bancoRepository;

    /**
     * Lista de bancos activos, ordenada por nombre.
     * Expuesta a todos los usuarios autenticados para poblar selects.
     */
    public List<BancoResponseDTO> listarActivos() {
        return bancoRepository.findByActivoTrueOrderByNombreAsc()
                .stream()
                .map(this::toDto)
                .toList();
    }

    /**
     * Obtener un banco por id (admin).
     */
    public Optional<BancoResponseDTO> obtenerPorId(Long id) {
        return bancoRepository.findById(id).map(this::toDto);
    }

    /**
     * Crear un nuevo banco.
     * Valida que el nombre no esté duplicado (case-insensitive).
     * El nuevo banco siempre arranca activo=true; esDefault=false por defecto.
     */
    @Transactional
    public BancoResponseDTO crear(BancoRequestDTO request) {
        String nombre = request.getNombre().trim();

        if (bancoRepository.existsByNombreIgnoreCase(nombre)) {
            throw new IllegalArgumentException(
                    "Ya existe un banco con el nombre '" + nombre + "'.");
        }

        Banco banco = new Banco();
        banco.setNombre(nombre);
        banco.setActivo(true);
        banco.setEsDefault(false);
        banco.setBancoDestino(resolverDestino(null, request.getBancoDestinoId()));

        Banco guardado = bancoRepository.save(banco);
        log.info("Banco creado: id={}, nombre='{}'", guardado.getId(), guardado.getNombre());
        return toDto(guardado);
    }

    /**
     * Actualizar un banco existente.
     *
     * Reglas:
     * - No se puede ser el propio banco destino.
     * - Si esDefault=true se activa este banco como default y se desmarca el anterior.
     * - Solo un banco puede tener esDefault=true al mismo tiempo.
     */
    @Transactional
    public BancoResponseDTO actualizar(Long id, BancoRequestDTO request) {
        Banco banco = bancoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Banco no encontrado (id=" + id + ")."));

        String nombre = request.getNombre().trim();

        // Validar nombre duplicado ignorando el propio registro
        Optional<Banco> existente = bancoRepository.findByNombreIgnoreCase(nombre);
        if (existente.isPresent() && !existente.get().getId().equals(id)) {
            throw new IllegalArgumentException(
                    "Ya existe otro banco con el nombre '" + nombre + "'.");
        }

        // Validar que no se señale a sí mismo como destino
        if (request.getBancoDestinoId() != null && request.getBancoDestinoId().equals(id)) {
            throw new IllegalArgumentException(
                    "Un banco no puede ser su propio banco destino.");
        }

        banco.setNombre(nombre);
        if (request.getActivo() != null) {
            banco.setActivo(request.getActivo());
        }
        banco.setBancoDestino(resolverDestino(id, request.getBancoDestinoId()));

        // Lógica de unicidad del default: si se marca este como default,
        // se desmarca el que estaba marcado antes (si es distinto).
        if (Boolean.TRUE.equals(request.getEsDefault())) {
            bancoRepository.findFirstByEsDefaultTrue().ifPresent(anteriorDefault -> {
                if (!anteriorDefault.getId().equals(id)) {
                    anteriorDefault.setEsDefault(false);
                    bancoRepository.save(anteriorDefault);
                }
            });
            banco.setEsDefault(true);
        } else if (request.getEsDefault() != null) {
            banco.setEsDefault(false);
        }

        Banco actualizado = bancoRepository.save(banco);
        log.info("Banco actualizado: id={}, nombre='{}', default={}, activo={}",
                actualizado.getId(), actualizado.getNombre(),
                actualizado.getEsDefault(), actualizado.getActivo());
        return toDto(actualizado);
    }

    // ------------------------------------------------------------------
    // Helpers privados
    // ------------------------------------------------------------------

    /**
     * Resuelve el banco destino a partir de su id.
     * idPropio se pasa para rechazar la auto-referencia (null cuando se crea).
     */
    private Banco resolverDestino(Long idPropio, Long bancoDestinoId) {
        if (bancoDestinoId == null) {
            return null;
        }
        if (bancoDestinoId.equals(idPropio)) {
            throw new IllegalArgumentException(
                    "Un banco no puede ser su propio banco destino.");
        }
        return bancoRepository.findById(bancoDestinoId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "El banco destino especificado no existe (id=" + bancoDestinoId + ")."));
    }

    private BancoResponseDTO toDto(Banco banco) {
        Long destinoId = banco.getBancoDestino() != null ? banco.getBancoDestino().getId() : null;
        String destinoNombre = banco.getBancoDestino() != null ? banco.getBancoDestino().getNombre() : null;
        return new BancoResponseDTO(
                banco.getId(),
                banco.getNombre(),
                banco.getActivo(),
                banco.getEsDefault(),
                destinoId,
                destinoNombre);
    }
}
