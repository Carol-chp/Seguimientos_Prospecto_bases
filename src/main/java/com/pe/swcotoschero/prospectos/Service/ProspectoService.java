package com.pe.swcotoschero.prospectos.Service;

import com.pe.swcotoschero.prospectos.Entity.Campania;
import com.pe.swcotoschero.prospectos.Entity.Personal;
import com.pe.swcotoschero.prospectos.Entity.Prospecto;
import com.pe.swcotoschero.prospectos.Repository.PersonalRepository;
import com.pe.swcotoschero.prospectos.Repository.ProspectoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@Service
public class ProspectoService {

    @Autowired
    private ProspectoRepository prospectoRepository;

    @Autowired
    private PersonalRepository personalRepository;

    public List<Prospecto> getAllProspectos() {
        return prospectoRepository.findAll();
    }

    public Prospecto saveProspecto(Prospecto prospecto) {
        return prospectoRepository.save(prospecto);
    }

    public void saveAllProspectos(List<Prospecto> prospectos) {
        prospectoRepository.saveAll(prospectos);
    }

    public List<Prospecto> listarTodos() {
        return prospectoRepository.findAll();
    }

    public Prospecto crearProspecto(Prospecto prospecto) {
        return prospectoRepository.save(prospecto);
    }

    public Prospecto actualizarProspecto(Long prospectoID, Prospecto prospecto) {
        prospecto.setProspectoID(prospectoID);
        return prospectoRepository.save(prospecto);
    }

    public void eliminarProspecto(Long prospectoID) {
        prospectoRepository.deleteById(prospectoID);
    }

    public void importarProspectosDesdeExcel(InputStream archivoExcel) throws IOException {

        BufferedReader reader = new BufferedReader(new InputStreamReader(archivoExcel));
        List<Prospecto> prospectos = new ArrayList<>();

        String linea;
        while ((linea = reader.readLine()) != null) {
            String[] datos = linea.split(",");
            Prospecto prospecto = new Prospecto();
            prospecto.setNombre(datos[0]);
            prospecto.setApellido(datos[1]);
            prospecto.setCelular(datos[2]);
            prospecto.setDocumentoIdentidad(datos[3]);
            prospecto.setSexo(datos[4]);
            prospecto.setBanco(datos[5]);
            prospecto.setCargo(datos[6]);
            prospecto.setDistrito(datos[7]);
            Campania campania = new Campania();
            campania.setDescripcion(datos[8]);
            prospecto.setCampania(campania);
            prospecto.setSubcampania(datos[9]);


            prospectos.add(prospecto);
        }

        // Aquí guardarías los prospectos en la base de datos.
         prospectoRepository.saveAll(prospectos);
    }
        // Workbook workbook = new XSSFWorkbook(archivoExcel);
        //Sheet sheet = workbook.getSheetAt(0);
       // for (Row row : sheet) {
           // if (row.getRowNum() == 0) continue; // Saltar encabezado
           // Prospecto prospecto = new Prospecto();
            //prospecto.setNombre(row.getCell(0).getStringCellValue());
            //prospecto.setApellidos(row.getCell(1).getStringCellValue());
           // prospecto.setCelular(row.getCell(2).getStringCellValue());
           // prospecto.setDocumentoIdentidad(row.getCell(3).getStringCellValue());
           // prospecto.setSexo(row.getCell(4).getStringCellValue());
          //  prospecto.setBanco(row.getCell(5).getStringCellValue());
          //  prospecto.setCargo(row.getCell(6).getStringCellValue());
          //  prospecto.setDistrito(row.getCell(7).getStringCellValue());
           // prospecto.setCampaña(row.getCell(8).getStringCellValue());
           // prospecto.setSubcampaña(row.getCell(9).getStringCellValue());

            //prospectoRepository.save(prospecto);
        //}
        //workbook.close();
   // }


    public void registrarContacto(Long prospectoId, String comentario) {
        // Buscar el prospecto por ID de forma no estática
        Prospecto prospecto = prospectoRepository.findById(prospectoId)
                .orElseThrow(() -> new IllegalArgumentException("El prospecto con ID " + prospectoId + " no existe"));

        // Agregar el comentario al prospecto
        prospecto.setComentario(comentario);

        // Guardar los cambios en la base de datos
        prospectoRepository.save(prospecto);
    }

    public boolean asignarProspecto(Long prospectoId, Long personalId) {
        // Buscar prospecto y personal en los repositorios
        Prospecto prospecto = prospectoRepository.findById(prospectoId).orElse(null);
        Personal personal = personalRepository.findById(personalId).orElse(null);

        // Verificar si ambos existen
        if (prospecto == null || personal == null) {
            return false; // Si alguno no existe, no realizar la asignación
        }

        // Realizar la asignación
        prospecto.setPersonal(String.valueOf(personalId));
        prospectoRepository.save(prospecto); // Guardar los cambios

        return true; // Si todo fue exitoso, retornar true
    }
}
