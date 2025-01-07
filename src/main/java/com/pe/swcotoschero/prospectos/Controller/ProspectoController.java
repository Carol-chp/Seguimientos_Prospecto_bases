package com.pe.swcotoschero.prospectos.Controller;

import com.pe.swcotoschero.prospectos.Service.ProspectoBusquedaService;
import com.pe.swcotoschero.prospectos.Service.ProspectoService;
import com.pe.swcotoschero.prospectos.dto.ProspectoBusquedaRequestDTO;
import com.pe.swcotoschero.prospectos.dto.ProspectoBusquedaResponseDTO;
import com.pe.swcotoschero.prospectos.helper.ExcelHelper;
import com.pe.swcotoschero.prospectos.Entity.Prospecto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

import java.util.List;

@RestController
@RequestMapping("/api/prospectos")
@Slf4j // Esto habilita el logger
public class ProspectoController {

    @Autowired
    private ProspectoService prospectoService;

    @Autowired
    private ProspectoBusquedaService prospectoBusquedaService;
    //private ProspectoRepository prospectoRepository; // Inyección del repositorio



    @PostMapping("/importar")
    public ResponseEntity<String> importarProspectosDesdeExcel(@RequestParam("file") MultipartFile file) {

        log.info("Archivo recibido: " + file.getOriginalFilename());
        try {
            // Validar si el archivo está vacío
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body("El archivo está vacío. Por favor, sube un archivo válido.");
            }

            // Validar si el archivo tiene formato .xlsx
            if (!file.getOriginalFilename().endsWith(".xlsx")) {
                return ResponseEntity.badRequest().body("Formato de archivo no soportado. Solo se permiten archivos con extensión .xlsx.");
            }

            // Procesar el archivo
            List<Prospecto> prospectos = prospectoService.importarProspectosDesdeExcel(file.getInputStream());
            return ResponseEntity.ok("Se importaron " + prospectos.size() + " prospectos correctamente.");
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Error al leer el archivo: " + e.getMessage());
        } catch (RuntimeException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al procesar el archivo: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error inesperado: " + e.getMessage());
        }
    }




    @GetMapping("/test")
    public ResponseEntity<String> testEndpoint() {
        return ResponseEntity.ok("El controlador funciona correctamente");
    }

    /*@PostMapping("/{id}/comentario")
    public ResponseEntity<Void> registrarComentario(@PathVariable Long id, @RequestBody String comentario) {
        prospectoService.registrarContacto(id, comentario);
        return ResponseEntity.ok().build();
    }*/


    @PreAuthorize("hasAnyRole('TELEOPERADOR', 'ADMINISTRADOR')")
    @GetMapping("/busqueda")
    public ResponseEntity<ProspectoBusquedaResponseDTO> buscarProspectos(
            @RequestParam(name = "campania", required = false, defaultValue = "") String campania,
            @RequestParam(name = "query", required = false, defaultValue = "") String textoBusqueda,
            @RequestParam(name = "pagina", required = false, defaultValue = "1") Integer pagina,
            @RequestParam(name = "tamanioPagina", required = false, defaultValue = "10") Integer tamanioPagina
    ) {


        return ResponseEntity.ok(prospectoBusquedaService.buscarProspectos(
                ProspectoBusquedaRequestDTO.builder()
                .campania(campania)
                .textoBusqueda(textoBusqueda)
                .pagina(pagina > 0 ? pagina - 1 : 0)
                .tamanioPagina(tamanioPagina)
                .build()));
    }

}
