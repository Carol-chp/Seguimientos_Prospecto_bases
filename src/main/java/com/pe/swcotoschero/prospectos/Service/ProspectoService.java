package com.pe.swcotoschero.prospectos.Service;

import com.pe.swcotoschero.prospectos.Entity.Banco;
import com.pe.swcotoschero.prospectos.Entity.Campania;
import com.pe.swcotoschero.prospectos.Entity.CargaMasiva;
import com.pe.swcotoschero.prospectos.Entity.Prospecto;
import com.pe.swcotoschero.prospectos.Repository.BancoRepository;
import com.pe.swcotoschero.prospectos.Repository.CampaniaRepository;
import com.pe.swcotoschero.prospectos.Repository.CargaMasivaRepository;
import com.pe.swcotoschero.prospectos.Repository.ProspectoRepository;
import com.pe.swcotoschero.prospectos.dto.ArchivoBase64Request;
import com.pe.swcotoschero.prospectos.dto.ImportacionResultDTO;
import com.pe.swcotoschero.prospectos.dto.LecturaExcelResultado;
import com.pe.swcotoschero.prospectos.dto.ProspectoDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
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
    private final BancoRepository bancoRepository;

    /** Tamaño máximo del archivo decodificado (defensa ante payloads enormes). */
    private static final int MAX_BYTES = 10 * 1024 * 1024; // 10 MB

    /**
     * Importa prospectos desde un Excel en Base64.
     * Valida tamaño, Base64, formato Excel y campos obligatorios por fila.
     * Lanza IllegalArgumentException (→ 400) ante cualquier entrada inválida.
     */
    public ImportacionResultDTO importartDesdeExcel(ArchivoBase64Request request) {
        if (request == null || request.getFileContent() == null
                || request.getFileContent().isBlank()) {
            throw new IllegalArgumentException("El contenido del archivo es obligatorio.");
        }

        LecturaExcelResultado lectura = obtenerProspectoDTOS(request);

        if (lectura.getValidos().isEmpty()) {
            throw new IllegalArgumentException(
                    "El archivo no contiene ninguna fila válida para importar."
                    + (lectura.getRechazadas().isEmpty()
                        ? ""
                        : " Filas rechazadas: " + lectura.getRechazadas().size() + "."));
        }

        CargaMasiva cargaMasiva = new CargaMasiva();
        cargaMasiva.setNombrearchivo(request.getFilename());
        cargaMasiva.setFecha(LocalDateTime.now());
        cargaMasiva.setEstadoAsignacion("SIN_ASIGNAR");
        cargaMasivaRepository.save(cargaMasiva);

        List<Prospecto> prospectosMapeados = mapFromDto(lectura.getValidos(), cargaMasiva);
        prospectoRepository.saveAll(prospectosMapeados);

        cargaMasiva.setCantidadProspectos(prospectosMapeados.size());
        cargaMasivaRepository.save(cargaMasiva);

        log.info("Importación: {} válidos, {} rechazados (carga {})",
                prospectosMapeados.size(), lectura.getRechazadas().size(), cargaMasiva.getId());

        return ImportacionResultDTO.builder()
                .success(true)
                .mensaje("Importación completada.")
                .cargaMasivaId(cargaMasiva.getId())
                .importados(prospectosMapeados.size())
                .rechazados(lectura.getRechazadas().size())
                .detalleRechazos(lectura.getRechazadas())
                .build();
    }

    private LecturaExcelResultado obtenerProspectoDTOS(ArchivoBase64Request request) {
        final byte[] decodedBytes;
        try {
            decodedBytes = Base64.getDecoder().decode(request.getFileContent());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("El contenido no es Base64 válido.");
        }

        if (decodedBytes.length == 0) {
            throw new IllegalArgumentException("El archivo está vacío.");
        }
        if (decodedBytes.length > MAX_BYTES) {
            throw new IllegalArgumentException(
                    "El archivo excede el tamaño máximo permitido (10 MB).");
        }

        File tempFile = null;
        try {
            tempFile = File.createTempFile("prospects-import-", ".xlsx");
            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                fos.write(decodedBytes);
            }
            return excelUploadService.leerExcel(tempFile);
        } catch (IOException e) {
            throw new IllegalArgumentException("No se pudo procesar el archivo: " + e.getMessage());
        } finally {
            if (tempFile != null && tempFile.exists() && !tempFile.delete()) {
                tempFile.deleteOnExit();
            }
        }
    }

    private List<Prospecto> mapFromDto(List<ProspectoDTO> prospectos, CargaMasiva cargaMasiva) {
        HashMap<String, Campania> campanias = new HashMap<>();
        List<Prospecto> result = new ArrayList<>();

        // Resolver banco default una sola vez por lote (evitar N consultas)
        Banco bancoDefault = bancoRepository.findFirstByEsDefaultTrue().orElse(null);
        if (bancoDefault == null) {
            log.warn("No hay banco default configurado; los prospectos importados quedarán sin banco.");
        }

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

        final Banco bancoFinal = bancoDefault;
        prospectos.forEach(prospecto -> {
            Prospecto p = new Prospecto();
            p.setNombre(prospecto.getNombre());
            p.setApellido(prospecto.getApellido());
            p.setCelular(prospecto.getCelular());
            p.setDocumentoIdentidad(prospecto.getDocumentoIdentidad());
            p.setCampania(campanias.get(prospecto.getCampania()));
            p.setDistrito(prospecto.getDistrito());
            p.setCargaMasiva(cargaMasiva);
            p.setBancoEntidad(bancoFinal);
            result.add(p);
        });
        return result;
    }
}
