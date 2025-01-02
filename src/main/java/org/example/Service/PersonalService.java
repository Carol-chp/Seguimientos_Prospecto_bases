package org.example.Service;

import org.example.Entity.Personal;
import org.example.Repository.PersonalRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PersonalService {

    @Autowired
    private PersonalRepository personalRepository;

    /**
     * Lista todos los registros de Personal.
     * @return Listado de Personal.
     */
    public List<Personal> listarTodos() {
        return personalRepository.findAll();
    }

    /**
     * Busca un registro de Personal por su ID.
     * @param id ID del Personal.
     * @return El Personal encontrado o null si no existe.
     */
    public Personal obtenerPorId(Long id) {
        Optional<Personal> personal = personalRepository.findById(id);
        return personal.orElse(null);
    }

    /**
     * Crea un nuevo registro de Personal.
     * @param personal Objeto de tipo Personal.
     * @return El Personal creado.
     */
    public Personal crearPersonal(Personal personal) {
        return personalRepository.save(personal);
    }

    /**
     * Actualiza un registro de Personal existente.
     * @param id ID del Personal a actualizar.
     * @param personal Objeto Personal con los nuevos datos.
     * @return El Personal actualizado o null si no existe.
     */
    public Personal actualizarPersonal(Long id, Personal personal) {
        if (personalRepository.existsById(id)) {
            personal.setPersonalID(id);
            return personalRepository.save(personal);
        }
        return null;
    }

    /**
     * Elimina un registro de Personal por su ID.
     * @param id ID del Personal a eliminar.
     */
    public void eliminarPersonal(Long id) {
        if (personalRepository.existsById(id)) {
            personalRepository.deleteById(id);
        }
    }
}
