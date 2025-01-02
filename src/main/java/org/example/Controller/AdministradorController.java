package org.example.Controller;

import org.example.Entity.Administrador;
import org.example.Service.AdministradorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/administradores")
public class AdministradorController {

    @Autowired
    private AdministradorService administradorService;

    @GetMapping
    public ResponseEntity<List<Administrador>> listarAdministradores() {
        return ResponseEntity.ok(administradorService.listarTodos());
    }

    @PostMapping
    public ResponseEntity<Administrador> crearAdministrador(@RequestBody Administrador administrador) {
        return ResponseEntity.ok(administradorService.crearAdministrador(administrador));
    }
}
