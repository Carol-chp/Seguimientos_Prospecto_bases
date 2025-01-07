package com.pe.swcotoschero.prospectos.Service;

import com.pe.swcotoschero.prospectos.Entity.Campania;
import com.pe.swcotoschero.prospectos.Entity.Personal;
import com.pe.swcotoschero.prospectos.Entity.Prospecto;
import com.pe.swcotoschero.prospectos.Repository.ProspectoRepository;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Service
public class ProspectoService {

    @Autowired
    private ProspectoRepository prospectoRepository;


    public List<Prospecto> getAllProspectos() {
        return prospectoRepository.findAll();
    }

    public Prospecto saveProspecto(Prospecto prospecto) {
        return prospectoRepository.save(prospecto);
    }

    public void saveAllProspectos(List<Prospecto> prospectos) {
        prospectoRepository.saveAll(prospectos);
    }





    public List<Prospecto> importarProspectosDesdeExcel(InputStream archivoExcel) throws IOException {
        List<Prospecto> prospectos = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(archivoExcel)) {
            Sheet sheet = workbook.getSheetAt(0);
            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue; // Saltar encabezado

                Prospecto prospecto = new Prospecto();
                prospecto.setNombre(getCellValue(row, 0));
                prospecto.setApellido(getCellValue(row, 1));
                prospecto.setCelular(getCellValue(row, 2));
                prospecto.setDocumentoIdentidad(getCellValue(row, 3));
                prospecto.setSexo(getCellValue(row, 4));
                prospecto.setBanco(getCellValue(row, 5));
                prospecto.setCargo(getCellValue(row, 6));
                prospecto.setDistrito(getCellValue(row, 7));
                Campania campania = new Campania();
                campania.setNombre(getCellValue(row, 8));
                prospecto.setCampania(campania);
                prospecto.setSubcampania(getCellValue(row, 9));

                // Validar campos obligatorios
                if (prospecto.getNombre().isEmpty() || prospecto.getCelular().isEmpty()) {
                    throw new IllegalArgumentException("El nombre y el celular son obligatorios.");
                }

                prospectos.add(prospecto);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error al procesar el archivo Excel: " + e.getMessage());
        }

        // Guardar todos los prospectos en una sola operación
        prospectoRepository.saveAll(prospectos);
        return prospectos;
    }



    // Método helper para leer celdas del archivo Excel
    private String getCellValue(Row row, int cellIndex) {
        if (row.getCell(cellIndex) == null) {
            return ""; // Retorna vacío si la celda está vacía
        }
        switch (row.getCell(cellIndex).getCellType()) {
            case STRING:
                return row.getCell(cellIndex).getStringCellValue().trim();
            case NUMERIC:
                return String.valueOf((long) row.getCell(cellIndex).getNumericCellValue());
            default:
                return ""; // Otros tipos no soportados
        }
    }


}
