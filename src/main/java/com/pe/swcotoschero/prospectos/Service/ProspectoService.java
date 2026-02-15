package com.pe.swcotoschero.prospectos.Service;

import com.pe.swcotoschero.prospectos.Entity.Campania;
import com.pe.swcotoschero.prospectos.Entity.CargaMasiva;
import com.pe.swcotoschero.prospectos.Entity.Prospecto;
import com.pe.swcotoschero.prospectos.Repository.CampaniaRepository;
import com.pe.swcotoschero.prospectos.Repository.CargaMasivaRepository;
import com.pe.swcotoschero.prospectos.Repository.ProspectoRepository;
import com.pe.swcotoschero.prospectos.dto.ArchivoBase64Request;
import com.pe.swcotoschero.prospectos.dto.ProspectoDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProspectoService {

    private final ProspectoRepository prospectoRepository;
    private final ExcelUploadService excelUploadService;
    private final CampaniaRepository campaniaRepository;
    private final CargaMasivaRepository cargaMasivaRepository;


    public List<Prospecto> getAllProspectos() {
        return prospectoRepository.findAll();
    }

    public Prospecto saveProspecto(Prospecto prospecto) {
        return prospectoRepository.save(prospecto);
    }

    public void saveAllProspectos(List<Prospecto> prospectos) {
        prospectoRepository.saveAll(prospectos);
    }




    public void importartDesdeExcel(ArchivoBase64Request request){
        try {

            List<ProspectoDTO> prospectos = obtenerProspectoDTOS(request);

            log.info("Archivo importado exitosamente");
            log.info("Prospectos importados: " + prospectos.size());

            // Creamos un registro de carga masiva
            CargaMasiva cargaMasiva = new CargaMasiva();
            cargaMasiva.setNombrearchivo(request.getFilename());
            cargaMasiva.setFecha(LocalDateTime.now());
            cargaMasiva.setCantidadProspectos(prospectos.size());
            cargaMasiva.setEstadoAsignacion("SIN_ASIGNAR");
            cargaMasivaRepository.save(cargaMasiva);


            List<Prospecto> prospectosMapeados = mapFromDto(prospectos, cargaMasiva);
            prospectoRepository.saveAll(prospectosMapeados);
            
            // Actualizar el conteo real de prospectos guardados
            cargaMasiva.setCantidadProspectos(prospectosMapeados.size());
            cargaMasivaRepository.save(cargaMasiva);
//

        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Error al decodificar el archivo Base64: " + e.getMessage());
        } catch (IOException e) {
            throw new RuntimeException("Error al procesar el archivo: " + e.getMessage());
        }
    }

    private List<ProspectoDTO> obtenerProspectoDTOS(ArchivoBase64Request request) throws IOException {
        // Decodificar el contenido Base64
        byte[] decodedBytes = Base64.getDecoder().decode(request.getFileContent());

        // Crear un archivo temporal para procesarlo
        File tempFile = File.createTempFile("temp-prospects", ".xlsx");
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.write(decodedBytes);

        }

        List<ProspectoDTO> prospectos = excelUploadService.leerExcel(tempFile);

        // Eliminar el archivo temporal después del uso
        tempFile.delete();
        return prospectos;
    }


    private List<Prospecto> mapFromDto(List<ProspectoDTO> prospectos, CargaMasiva cargaMasiva) {
        HashMap<String, Campania> campanias = new HashMap<>();
        List<Prospecto> result = new ArrayList<>();
        List<String> campaniasNombres = prospectos.stream().map(ProspectoDTO::getCampania).toList();
        campaniasNombres.forEach(campaniaNombre -> {
            Campania campania = campaniaRepository.findByNombre(campaniaNombre).orElse(null);
            if (campania == null) {
                campania = new Campania();
                campania.setNombre(campaniaNombre);
                campania.setDescripcion(campaniaNombre);
                campaniaRepository.save(campania);
            }
            campanias.put(campaniaNombre, campania);
        });
        prospectos.forEach(prospecto -> {
            Prospecto p = new Prospecto();
            p.setNombre(prospecto.getNombre());
            p.setApellido(prospecto.getApellido());
            p.setCelular(prospecto.getCelular());
            p.setDocumentoIdentidad(prospecto.getDocumentoIdentidad());
            p.setCampania(campanias.get(prospecto.getCampania()));
            p.setDistrito(prospecto.getDistrito());
            p.setCargaMasiva(cargaMasiva);
            result.add(p);
        });
        return result;
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
