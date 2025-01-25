package com.pe.swcotoschero.prospectos.Service;

import com.pe.swcotoschero.prospectos.Entity.Prospecto;
import com.pe.swcotoschero.prospectos.dto.ProspectoDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class ExcelUploadService {

    public List<ProspectoDTO> leerExcel(File file) {
        List<ProspectoDTO> prospectos = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = WorkbookFactory.create(fis)) {

            // Obtener la primera hoja
            Sheet sheet = workbook.getSheetAt(0);

            // Iterar sobre las filas, saltando la primera (cabecera)
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row != null) {
                    ProspectoDTO prospecto = ProspectoDTO.builder()
                            .nombre(obtenerCeldaComoTexto(row, 0).replaceAll(" ", ""))
                            .apellido(obtenerCeldaComoTexto(row, 1).replaceAll(" ", ""))
                            .campania(obtenerCeldaComoTexto(row, 2).replaceAll(" ", ""))
                            .documentoIdentidad(obtenerCeldaComoTexto(row, 3).replaceAll(" ", ""))
                            .celular(obtenerCeldaComoTexto(row, 4).replaceAll(" ", ""))
                            .build();
                    prospectos.add(prospecto);
                }
            }

        } catch (IOException e) {
            log.error("Error al leer el archivo Excel: " + e.getMessage(), e);
        }
        return prospectos;
    }

    private String obtenerCeldaComoTexto(Row row, int index) {
        Cell cell = row.getCell(index);
        if (cell == null) {
            return "";
        }
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> "";
        };
    }
}
