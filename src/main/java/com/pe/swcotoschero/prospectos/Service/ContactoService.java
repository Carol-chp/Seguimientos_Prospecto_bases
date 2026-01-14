package com.pe.swcotoschero.prospectos.Service;

import com.pe.swcotoschero.prospectos.Entity.Asignacion;
import com.pe.swcotoschero.prospectos.Entity.Contacto;
import com.pe.swcotoschero.prospectos.Entity.Prospecto;
import com.pe.swcotoschero.prospectos.Repository.AsignacionRepository;
import com.pe.swcotoschero.prospectos.Repository.ContactoRepository;
import com.pe.swcotoschero.prospectos.Repository.ProspectoRepository;
import com.pe.swcotoschero.prospectos.dto.ContactoRegistroDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ContactoService {
    @Autowired
    private ContactoRepository contactoRepository;
    
    @Autowired
    private AsignacionRepository asignacionRepository;
    
    @Autowired
    private ProspectoRepository prospectoRepository;

    public List<Contacto> listarTodos() {
        return contactoRepository.findAll();
    }

    public Optional<Contacto> obtenerPorId(Long id) {
        return contactoRepository.findById(id);
    }

    public Contacto guardar(Contacto contacto) {
        return contactoRepository.save(contacto);
    }

    public void eliminar(Long id) {
        contactoRepository.deleteById(id);
    }

    @Transactional
    public void registrarContacto(ContactoRegistroDTO contactoDTO) {
        // Buscar la asignación del prospecto
        Asignacion asignacion = asignacionRepository.findByProspecto_ProspectoID(contactoDTO.getProspectoId())
                .orElseThrow(() -> new IllegalArgumentException("No se encontró asignación para el prospecto con ID " + contactoDTO.getProspectoId()));

        // Crear nuevo registro de contacto (histórico)
        Contacto contacto = new Contacto();
        contacto.setAsignacion(asignacion);
        contacto.setComentario(contactoDTO.getComentario());
        contacto.setContestoLlamada(contactoDTO.getContestoLlamada());
        contacto.setInteresado(contactoDTO.getInteresado());
        contacto.setFechaContacto(LocalDateTime.now());

        // Guardar el registro histórico
        contactoRepository.save(contacto);

        // Actualizar el estado del prospecto con el último valor de interesado
        Prospecto prospecto = asignacion.getProspecto();
        prospecto.setEstadoInteresado(contactoDTO.getInteresado());
        prospectoRepository.save(prospecto);
    }
}
