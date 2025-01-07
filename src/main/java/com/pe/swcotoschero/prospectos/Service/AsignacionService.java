package com.pe.swcotoschero.prospectos.Service;

import com.pe.swcotoschero.prospectos.Entity.Asignacion;
import com.pe.swcotoschero.prospectos.Entity.Personal;
import com.pe.swcotoschero.prospectos.Entity.Prospecto;
import com.pe.swcotoschero.prospectos.Repository.AsignacionRepository;
import com.pe.swcotoschero.prospectos.Repository.ProspectoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AsignacionService {
    @Autowired
    private AsignacionRepository asignacionRepository;
//
//    @Autowired
//    private PersonalRepository personalRepository;

    @Autowired
    private ProspectoRepository prospectoRepository;

    public List<Asignacion> listarTodos() {
        return asignacionRepository.findAll();
    }

    public Optional<Asignacion> obtenerPorId(Long id) {
        return asignacionRepository.findById(id);
    }

    public Asignacion guardar(Asignacion asignacion) {
        return asignacionRepository.save(asignacion);
    }

    public void eliminar(Long id) {
        asignacionRepository.deleteById(id);
    }

    /*public boolean asignarProspecto(Long prospectoId, Long personalId) {
        // Buscar prospecto y personal
        Prospecto prospecto = prospectoRepository.findById(prospectoId).orElse(null);
//        Personal personal = personalRepository.findById(personalId).orElse(null);

        // Verificar si ambos existen
//        if (prospecto == null || personal == null) {
//            return false;  // Si alguno no existe, no realizar la asignación
//        }

        // Realizar la asignación
       prospecto.setPersonal(String.valueOf(personalId));
        prospectoRepository.save(prospecto);  // Guardar los cambios

        return true;  // Si todo fue exitoso, retornar true
    }*/
}
