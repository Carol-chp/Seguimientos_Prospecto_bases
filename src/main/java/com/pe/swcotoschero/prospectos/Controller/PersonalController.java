package com.pe.swcotoschero.prospectos.Controller;

import com.pe.swcotoschero.prospectos.Service.PersonalService;
import com.pe.swcotoschero.prospectos.Entity.Personal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/personal")
public class PersonalController {

    @Autowired
    private PersonalService personalService;

    @GetMapping
    public ResponseEntity<List<Personal>> listarPersonal() {
        return ResponseEntity.ok(personalService.listarTodos());
    }

    @PostMapping
    public ResponseEntity<Personal> crearPersonal(@RequestBody Personal personal) {
        return ResponseEntity.ok(personalService.crearPersonal(personal));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Personal> actualizarPersonal(@PathVariable Long id, @RequestBody Personal personal) {
        return ResponseEntity.ok(personalService.actualizarPersonal(id, personal));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> eliminarPersonal(@PathVariable Long id) {
        personalService.eliminarPersonal(id);
        return ResponseEntity.ok("Personal eliminado exitosamente.");
    }
}
