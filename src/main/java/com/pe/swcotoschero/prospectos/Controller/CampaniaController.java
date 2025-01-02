package com.pe.swcotoschero.prospectos.Controller;

import com.pe.swcotoschero.prospectos.Service.CampaniaService;
import com.pe.swcotoschero.prospectos.Entity.Campania;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/campanias")
public class CampaniaController {

    @Autowired
    private CampaniaService campaniaService;

    @GetMapping
    public ResponseEntity<List<Campania>> listarCampanias() {
        return ResponseEntity.ok(campaniaService.listarTodos());
    }

    @PostMapping
    public ResponseEntity<Campania> crearCampania(@RequestBody Campania campaña) {
        return ResponseEntity.ok(campaniaService.crearCampania(campaña));
    }
}
