package org.example.Service;

import org.example.Entity.Administrador;
import org.example.Repository.AdministradorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AdministradorService {

    @Autowired
    private AdministradorRepository administradorRepository;

    public List<Administrador> listarTodos() {
        return administradorRepository.findAll();
    }

    public Optional<Administrador> obtenerPorId(Long id) {
        return administradorRepository.findById(id);
    }

    public Administrador guardar(Administrador administrador) {
        return administradorRepository.save(administrador);
    }

    public void eliminar(Long id) {
        administradorRepository.deleteById(id);
    }

    public Administrador crearAdministrador(Administrador administrador) {
        // Validación opcional antes de guardar
        if (administrador == null || administrador.getNombre() == null || administrador.getEmail() == null) {
            throw new IllegalArgumentException("Los datos del administrador no son válidos");
        }
        return administradorRepository.save(administrador);
    }
}
