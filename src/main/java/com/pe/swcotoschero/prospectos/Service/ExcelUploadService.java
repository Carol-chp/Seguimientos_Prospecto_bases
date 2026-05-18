package com.pe.swcotoschero.prospectos.Service;

import com.pe.swcotoschero.prospectos.dto.FilaRechazada;
import com.pe.swcotoschero.prospectos.dto.LecturaExcelResultado;
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
import java.util.ArrayList;
import java.util.List;

/**
 * Lectura y validación de Excel de prospectos.
 *
 * Columnas esperadas (fila 0 = cabecera, se ignora):
 *   0=nombre  1=apellido  2=campania  3=documentoIdentidad  4=celular
 *
 * - NO se eliminan espacios internos (antes `replaceAll(" ","")` rompía nombres
 *   compuestos como "Juan Pablo"); solo se hace trim.
 * - Filas totalmente vacías se ignoran en silencio (relleno final del archivo).
 * - Filas con algún dato pero campos obligatorios faltantes se rechazan con
 *   motivo (no se importan a medias).
 * - Si el archivo no es un Excel legible, se lanza IllegalArgumentException
 *   (el GlobalExceptionHandler lo traduce a 400).
 */
@Service
@Slf4j
public class ExcelUploadService {

    private static final int COL_NOMBRE = 0;
    private static final int COL_APELLIDO = 1;
    private static final int COL_CAMPANIA = 2;
    private static final int COL_DOCUMENTO = 3;
    private static final int COL_CELULAR = 4;

    public LecturaExcelResultado leerExcel(File file) {
        LecturaExcelResultado resultado = new LecturaExcelResultado();

        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = WorkbookFactory.create(fis)) {

            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                throw new IllegalArgumentException("El Excel no tiene hojas.");
            }

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }

                String nombre = celda(row, COL_NOMBRE);
                String apellido = celda(row, COL_APELLIDO);
                String campania = celda(row, COL_CAMPANIA);
                String documento = celda(row, COL_DOCUMENTO);
                String celular = celda(row, COL_CELULAR);

                // Fila completamente vacía → se ignora (relleno al final)
                if (nombre.isEmpty() && apellido.isEmpty() && campania.isEmpty()
                        && documento.isEmpty() && celular.isEmpty()) {
                    continue;
                }

                List<String> faltantes = new ArrayList<>();
                if (nombre.isEmpty()) faltantes.add("nombre");
                if (apellido.isEmpty()) faltantes.add("apellido");
                if (campania.isEmpty()) faltantes.add("campania");
                if (documento.isEmpty()) faltantes.add("documentoIdentidad");
                if (celular.isEmpty()) faltantes.add("celular");

                if (!faltantes.isEmpty()) {
                    resultado.getRechazadas().add(new FilaRechazada(
                            i + 1, "Campos obligatorios faltantes: " + String.join(", ", faltantes)));
                    continue;
                }

                resultado.getValidos().add(ProspectoDTO.builder()
                        .nombre(nombre)
                        .apellido(apellido)
                        .campania(campania)
                        .documentoIdentidad(documento)
                        .celular(celular)
                        .build());
            }

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            // POI lanza si el contenido no es un Excel válido.
            log.warn("Archivo no es un Excel válido: {}", e.getMessage());
            throw new IllegalArgumentException(
                    "El archivo no es un Excel (.xlsx/.xls) válido o está corrupto.");
        }

        return resultado;
    }

    /** Valor de la celda como texto, con trim. Nunca null. */
    private String celda(Row row, int index) {
        Cell cell = row.getCell(index);
        if (cell == null) {
            return "";
        }
        String valor = switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> {
                double d = cell.getNumericCellValue();
                // DNI/celular son enteros: evitar notación científica "1.23E8"
                yield (d == Math.floor(d) && !Double.isInfinite(d))
                        ? String.valueOf((long) d)
                        : String.valueOf(d);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> "";
        };
        return valor == null ? "" : valor.trim();
    }
}
