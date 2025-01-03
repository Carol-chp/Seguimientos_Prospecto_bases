package com.pe.swcotoschero.prospectos.Service;

import com.pe.swcotoschero.prospectos.Entity.Prospecto;
import com.pe.swcotoschero.prospectos.Repository.ProspectoRepository;
import com.pe.swcotoschero.prospectos.dto.ProspectoBusquedaRequestDTO;
import com.pe.swcotoschero.prospectos.dto.ProspectoBusquedaResponseDTO;
import com.pe.swcotoschero.prospectos.dto.ProspectoDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProspectoBusquedaService {


    private final ProspectoRepository prospectoRepository;

    public ProspectoBusquedaResponseDTO buscarProspectos(ProspectoBusquedaRequestDTO request) {

        Pageable pageable = PageRequest.of(request.getPagina(), request.getTamanioPagina());
        Page<Prospecto> page = prospectoRepository.findProspectos(request.getCampania(), request.getTextoBusqueda(), pageable);

        log.info("Pageable: " + page.getContent().size());
        return ProspectoBusquedaResponseDTO.builder()
                .pagina(page.getNumber() + 1)
                .tamanioPagina(page.getSize())
                .total(page.getTotalElements())
                .resultados(page.getContent().stream().map(entity -> ProspectoDTO.builder()
                        .id(entity.getProspectoID())
                        .nombre(entity.getNombre())
                        .apellido(entity.getApellido())
                        .celular(entity.getCelular())
                        .documentoIdentidad(entity.getDocumentoIdentidad())
                        .sexo(entity.getSexo())
                        .cargo(entity.getCargo())
                        .distrito(entity.getDistrito())
                        .campania(entity.getCampania().getDescripcion())
                        .subcampania(entity.getSubcampania())
                        .build()).collect(java.util.stream.Collectors.toList()))
                .build();
    }
}
