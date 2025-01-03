package com.pe.swcotoschero.prospectos.Service;

import com.pe.swcotoschero.prospectos.Entity.Campania;
import com.pe.swcotoschero.prospectos.Repository.CampaniaRepository;
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
        if (campania == null || campania.getId() == null ) {
            throw new IllegalArgumentException("Los datos del administrador no son válidos");
        }
        return campaniaRepository.save(campania);
    }
}
