package com.pe.swcotoschero.prospectos.Controller;

import com.pe.swcotoschero.prospectos.Service.ProspectoBusquedaService;
import com.pe.swcotoschero.prospectos.Service.ProspectoService;
import com.pe.swcotoschero.prospectos.dto.ArchivoBase64Request;
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import java.util.Base64;
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
    public ResponseEntity<String> importarProspectosDesdeBase64(@RequestBody ArchivoBase64Request request) {
        try {
            // Decodificar el contenido Base64
            byte[] decodedBytes = Base64.getDecoder().decode(request.getFileContent());

            // Crear un archivo temporal para procesarlo
            File tempFile = File.createTempFile("temp-prospects", ".xlsx");
            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                fos.write(decodedBytes);
            }

            // Aquí puedes procesar el archivo como prefieras
            System.out.println("Archivo procesado: " + tempFile.getAbsolutePath());

            // Eliminar el archivo temporal después del uso
            tempFile.delete();

            return ResponseEntity.ok("Archivo importado exitosamente");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Error al decodificar el archivo Base64: " + e.getMessage());
        } catch (IOException e) {
            return ResponseEntity.status(500).body("Error al procesar el archivo: " + e.getMessage());
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
