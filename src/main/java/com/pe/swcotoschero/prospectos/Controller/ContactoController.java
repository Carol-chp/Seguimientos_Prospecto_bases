package com.pe.swcotoschero.prospectos.Controller;

import com.pe.swcotoschero.prospectos.Service.ContactoService;
import com.pe.swcotoschero.prospectos.dto.ContactoRegistroDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/contactos")
public class ContactoController {

    @Autowired
    private ContactoService contactoService;
    
    @PostMapping
    public ResponseEntity<String> registrarContacto(@RequestBody ContactoRegistroDTO contactoDTO) {
        contactoService.registrarContacto(contactoDTO);
        return ResponseEntity.ok("Contacto registrado exitosamente.");
    }
}
