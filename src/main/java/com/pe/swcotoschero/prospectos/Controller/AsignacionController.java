package com.pe.swcotoschero.prospectos.Controller;
import com.pe.swcotoschero.prospectos.Service.AsignacionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/asignaciones")

public class AsignacionController {

    @Autowired
    private AsignacionService asignacionService;

   /* @PostMapping
    public ResponseEntity<String> asignarProspecto(@RequestParam Long prospectoId, @RequestParam Long personalId) {
        boolean asignado = asignacionService.asignarProspecto(prospectoId, personalId);

        if (asignado) {
            return ResponseEntity.ok("Asignación realizada exitosamente.");
        } else {
            return ResponseEntity.status(400).body("Error al realizar la asignación. Verifique los datos.");
        }
    }*/
}
