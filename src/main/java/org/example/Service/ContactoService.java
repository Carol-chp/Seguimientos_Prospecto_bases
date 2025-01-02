package org.example.Service;

import org.example.Entity.Contacto;
import org.example.Entity.Prospecto;
import org.example.Repository.ContactoRepository;
import org.example.Repository.ProspectoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ContactoService {
    @Autowired
    private ContactoRepository contactoRepository;

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

    public void registrarContacto(Long contactoId, String comentario) {
        // Buscar el contacto por ID de forma no estática
        Contacto contacto = contactoRepository.findById(contactoId)
                .orElseThrow(() -> new IllegalArgumentException("El contacto con ID " + contactoId + " no existe"));

        // Agregar el comentario al contacto
        contacto.setComentario(comentario);

        // Guardar los cambios en la base de datos
        contactoRepository.save(contacto);
    }
}
