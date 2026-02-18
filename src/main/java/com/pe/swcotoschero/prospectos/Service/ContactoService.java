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
import java.util.Set;

@Service
public class ContactoService {
    @Autowired
    private ContactoRepository contactoRepository;

    @Autowired
    private AsignacionRepository asignacionRepository;

    @Autowired
    private ProspectoRepository prospectoRepository;

    private static final Set<String> ESTADOS_FINALES = Set.of("CONCRETO_PRESTAMO", "NO_VOLVER_LLAMAR");

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

        String estadoResultado = contactoDTO.getEstadoResultado();

        // Crear nuevo registro de contacto (histórico)
        Contacto contacto = new Contacto();
        contacto.setAsignacion(asignacion);
        contacto.setComentario(contactoDTO.getComentario());
        contacto.setFechaContacto(LocalDateTime.now());
        contacto.setEstadoResultado(estadoResultado);

        // Retrocompatibilidad con campos booleanos
        if (estadoResultado != null) {
            contacto.setContestoLlamada(!"NO_CONTESTO".equals(estadoResultado));
            contacto.setInteresado("PROSPECTO".equals(estadoResultado) || "CONCRETO_PRESTAMO".equals(estadoResultado));
        } else {
            contacto.setContestoLlamada(contactoDTO.getContestoLlamada());
            contacto.setInteresado(contactoDTO.getInteresado());
        }

        contactoRepository.save(contacto);

        // Actualizar estado de la asignacion
        if (estadoResultado != null) {
            asignacion.setEstadoResultado(estadoResultado);

            // Actualizar estado de gestion
            if (ESTADOS_FINALES.contains(estadoResultado)) {
                asignacion.setEstado("FINALIZADO");
            } else {
                asignacion.setEstado("EN_GESTION");
            }

            // Manejar fecha de agenda
            if ("AGENDADO".equals(estadoResultado) && contactoDTO.getFechaAgenda() != null) {
                asignacion.setFechaAgenda(LocalDateTime.parse(contactoDTO.getFechaAgenda()));
            } else {
                asignacion.setFechaAgenda(null);
            }

            asignacionRepository.save(asignacion);
        }

        // Actualizar estado interesado del prospecto
        Prospecto prospecto = asignacion.getProspecto();
        boolean interesado = "PROSPECTO".equals(estadoResultado) || "CONCRETO_PRESTAMO".equals(estadoResultado);
        prospecto.setEstadoInteresado(interesado);
        prospectoRepository.save(prospecto);
    }
}
