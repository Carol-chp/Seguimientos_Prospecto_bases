package org.example.Controller;

import org.example.Entity.Prospecto;
import org.example.Repository.ProspectoRepository;
import org.example.helper.ExcelHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.example.Service.ProspectoService;

import java.io.IOException;
import java.io.InputStream;

import java.util.List;

@RestController
@RequestMapping("/api/prospectos")
public class ProspectoController {

    @Autowired
    private ProspectoService prospectoService;
    //private ProspectoRepository prospectoRepository; // Inyección del repositorio

    //@GetMapping
   // public ResponseEntity<List<Prospecto>> listarProspectos() {
        //return ResponseEntity.ok(prospectoService.listarTodos());
   // }

    @GetMapping
    public ResponseEntity<List<Prospecto>> getAllProspectos() {
        return ResponseEntity.ok(prospectoService.getAllProspectos());
    }

    @PostMapping("/upload")
    public ResponseEntity<String> uploadProspectos(@RequestParam("file") MultipartFile file) {
        try {
            List<Prospecto> prospectos = ExcelHelper.parseExcel(file.getInputStream());
            prospectoService.saveAllProspectos(prospectos);
            return ResponseEntity.ok("Archivo procesado y prospectos guardados correctamente.");
        } catch (IOException e) {
            return ResponseEntity.badRequest().body("Error al procesar el archivo.");
        }
    }

   // @PostMapping
   // public ResponseEntity<Prospecto> crearProspecto(@RequestBody Prospecto prospecto) {
     //   return ResponseEntity.ok(prospectoService.crearProspecto(prospecto));
  //  }

    @PostMapping
    public ResponseEntity<String> crearProspecto(@RequestBody Prospecto prospecto) {


        // Lógica para crear un nuevo prospecto
        // Guardar el prospecto en la base de datos
        //prospectoRepository.save(prospecto);

        return ResponseEntity.status(HttpStatus.CREATED).body("Prospecto creado correctamente");
    }

    @PutMapping("/{id}")
    public ResponseEntity<Prospecto> actualizarProspecto(@PathVariable Long id, @RequestBody Prospecto prospecto) {
        return ResponseEntity.ok(prospectoService.actualizarProspecto(id, prospecto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> eliminarProspecto(@PathVariable Long id) {
        prospectoService.eliminarProspecto(id);
        return ResponseEntity.ok("Prospecto eliminado exitosamente.");
    }


    @PostMapping("/importar")
    public ResponseEntity<String> importarArchivo(@RequestParam("archivo") MultipartFile archivo) {
        try (InputStream archivoExcel = archivo.getInputStream()) {
            prospectoService.importarProspectosDesdeExcel(archivoExcel);
            return ResponseEntity.ok("Archivo importado exitosamente");
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al importar archivo");
        }
    }



    @GetMapping("/test")
    public ResponseEntity<String> testEndpoint() {
        return ResponseEntity.ok("El controlador funciona correctamente");
    }

    @PostMapping("/{id}/comentario")
    public ResponseEntity<Void> registrarComentario(@PathVariable Long id, @RequestBody String comentario) {
        prospectoService.registrarContacto(id, comentario);
        return ResponseEntity.ok().build();
    }

}
