package com.pe.swcotoschero.prospectos.helper;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import com.pe.swcotoschero.prospectos.Entity.Prospecto;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class ExcelHelper {

    public static List<Prospecto> parseExcel(InputStream is) {
        List<Prospecto> prospectos = new ArrayList<>();
        try (Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue; // Saltar encabezado
                Prospecto prospecto = new Prospecto();
                prospecto.setNombre(row.getCell(0).getStringCellValue());
                prospecto.setApellidos(row.getCell(1).getStringCellValue());
                prospecto.setCelular(row.getCell(2).getStringCellValue());
                prospecto.setDocumentoIdentidad(row.getCell(3).getStringCellValue());
                prospecto.setSexo(row.getCell(4).getStringCellValue());
                prospecto.setBanco(row.getCell(5).getStringCellValue());
                prospecto.setCargo(row.getCell(6).getStringCellValue());
                prospecto.setDistrito(row.getCell(7).getStringCellValue());
                prospecto.setCampania(row.getCell(8).getStringCellValue());
                prospecto.setSubcampania(row.getCell(9).getStringCellValue());
                prospectos.add(prospecto);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return prospectos;
    }
}
