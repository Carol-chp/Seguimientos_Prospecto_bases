package org.example.Controller;

import org.example.Service.ContactoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/contactos")
public class ContactoController {

    @Autowired
    private ContactoService contactoService;
    @PostMapping
    public ResponseEntity<String> registrarContacto(@RequestParam Long prospectoId, @RequestParam String comentario) {
        contactoService.registrarContacto(prospectoId, comentario);
        return ResponseEntity.ok("Contacto registrado exitosamente.");
    }
}
