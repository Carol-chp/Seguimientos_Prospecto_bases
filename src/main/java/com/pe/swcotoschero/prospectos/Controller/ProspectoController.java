package com.pe.swcotoschero.prospectos.Controller;

import com.pe.swcotoschero.prospectos.Service.ProspectoBusquedaService;
import com.pe.swcotoschero.prospectos.Service.ProspectoService;
import com.pe.swcotoschero.prospectos.dto.ArchivoBase64Request;
import com.pe.swcotoschero.prospectos.dto.ImportacionResultDTO;
import com.pe.swcotoschero.prospectos.dto.ProspectoBusquedaRequestDTO;
import com.pe.swcotoschero.prospectos.dto.ProspectoBusquedaResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/prospectos")
@Slf4j
public class ProspectoController {

    @Autowired
    private ProspectoService prospectoService;

    @Autowired
    private ProspectoBusquedaService prospectoBusquedaService;

    /**
     * Importacion masiva de prospectos desde Excel (Base64).
     * Restringido al rol ADMINISTRADOR (Fase 0.3).
     */
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @PostMapping("/importar")
    public ResponseEntity<ImportacionResultDTO> importarProspectosDesdeBase64(
            @RequestBody ArchivoBase64Request request) {
        // Entradas inválidas lanzan IllegalArgumentException →
        // GlobalExceptionHandler responde 400 con JSON {message,...}.
        return ResponseEntity.ok(prospectoService.importartDesdeExcel(request));
    }

    // GET /api/prospectos/test eliminado intencionalmente (Fase 0.3).

    @PreAuthorize("hasAnyRole('TELEOPERADOR', 'ADMINISTRADOR')")
    @GetMapping("/busqueda")
    public ResponseEntity<ProspectoBusquedaResponseDTO> buscarProspectos(
            @RequestParam(name = "campania", required = false, defaultValue = "") String campania,
            @RequestParam(name = "query", required = false, defaultValue = "") String textoBusqueda,
            @RequestParam(name = "pagina", required = false, defaultValue = "1") Integer pagina,
            @RequestParam(name = "tamanioPagina", required = false, defaultValue = "10") Integer tamanioPagina) {

        return ResponseEntity.ok(prospectoBusquedaService.buscarProspectos(
                ProspectoBusquedaRequestDTO.builder()
                        .campania(campania)
                        .textoBusqueda(textoBusqueda)
                        .pagina(pagina > 0 ? pagina - 1 : 0)
                        .tamanioPagina(tamanioPagina)
                        .build()));
    }

    @PreAuthorize("hasAnyRole('TELEOPERADOR', 'ADMINISTRADOR')")
    @GetMapping("/interesados")
    public ResponseEntity<ProspectoBusquedaResponseDTO> buscarProspectosInteresados(
            @RequestParam(name = "campania", required = false, defaultValue = "") String campania,
            @RequestParam(name = "query", required = false, defaultValue = "") String textoBusqueda,
            @RequestParam(name = "pagina", required = false, defaultValue = "1") Integer pagina,
            @RequestParam(name = "tamanioPagina", required = false, defaultValue = "10") Integer tamanioPagina) {

        return ResponseEntity.ok(prospectoBusquedaService.buscarProspectosInteresados(
                ProspectoBusquedaRequestDTO.builder()
                        .campania(campania)
                        .textoBusqueda(textoBusqueda)
                        .pagina(pagina > 0 ? pagina - 1 : 0)
                        .tamanioPagina(tamanioPagina)
                        .build()));
    }
}
