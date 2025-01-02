package org.example.Service;

import org.example.Entity.Administrador;
import org.example.Entity.Campania;
import org.example.Repository.CampaniaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CampaniaService {
    @Autowired
    private CampaniaRepository campaniaRepository;

    public List<Campania> listarTodos() {
        return campaniaRepository.findAll();
    }

    public Optional<Campania> obtenerPorId(Long id) {
        return campaniaRepository.findById(id);
    }

    public Campania guardar(Campania campania) {
        return campaniaRepository.save(campania);
    }

    public void eliminar(Long id) {
        campaniaRepository.deleteById(id);
    }

    public Campania crearCampania(Campania campania) {
        // Validación opcional antes de guardar
        if (campania == null || campania.getCampaniaID() == null ) {
            throw new IllegalArgumentException("Los datos del administrador no son válidos");
        }
        return campaniaRepository.save(campania);
    }
}
