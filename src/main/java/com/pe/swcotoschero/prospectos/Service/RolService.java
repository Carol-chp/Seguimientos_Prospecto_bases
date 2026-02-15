package com.pe.swcotoschero.prospectos.Service;

import com.pe.swcotoschero.prospectos.Entity.Rol;
import com.pe.swcotoschero.prospectos.Repository.RolRepository;
import com.pe.swcotoschero.prospectos.dto.RolDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RolService {

    private final RolRepository rolRepository;

    /**
     * Obtener todos los roles disponibles
     *
     * @return Lista de RolDTO con todos los roles
     */
    public List<RolDTO> obtenerTodosLosRoles() {
        try {
            log.info("Obteniendo todos los roles");
            List<Rol> roles = rolRepository.findAll();
            log.info("Se encontraron {} roles", roles.size());

            return roles.stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error al obtener roles: {}", e.getMessage(), e);
            throw new RuntimeException("Error al obtener los roles disponibles");
        }
    }

    /**
     * Convertir entidad Rol a DTO
     */
    private RolDTO convertirADTO(Rol rol) {
        return new RolDTO(rol.getId(), rol.getNombre());
    }
}
