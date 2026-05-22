package com.pe.swcotoschero.prospectos.Service;

import com.pe.swcotoschero.prospectos.Entity.Asignacion;
import com.pe.swcotoschero.prospectos.Entity.CargaMasiva;
import com.pe.swcotoschero.prospectos.Entity.Prospecto;
import com.pe.swcotoschero.prospectos.Entity.Usuario;
import com.pe.swcotoschero.prospectos.Entity.enums.EstadoGestion;
import com.pe.swcotoschero.prospectos.Repository.AsignacionRepository;
import com.pe.swcotoschero.prospectos.Repository.CargaMasivaRepository;
import com.pe.swcotoschero.prospectos.Repository.ProspectoRepository;
import com.pe.swcotoschero.prospectos.Repository.UsuarioRepository;
import com.pe.swcotoschero.prospectos.dto.AsignacionMultiRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class AsignacionService {

    @Autowired
    private AsignacionRepository asignacionRepository;

    @Autowired
    private ProspectoRepository prospectoRepository;

    @Autowired
    private CargaMasivaRepository cargaMasivaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private CargaMasivaService cargaMasivaService;

    public List<Asignacion> listarTodos() {
        return asignacionRepository.findAll();
    }

    public Optional<Asignacion> obtenerPorId(Long id) {
        return asignacionRepository.findById(id);
    }

    public Asignacion guardar(Asignacion asignacion) {
        return asignacionRepository.save(asignacion);
    }

    public void eliminar(Long id) {
        asignacionRepository.deleteById(id);
    }

    /**
     * Asigna prospectos de una carga masiva a un usuario especifico.
     * Solo asigna prospectos que aun no tienen asignacion activa.
     * Estado inicial canonico: SIN_GESTIONAR.
     *
     * @param cargaMasivaId   ID de la carga masiva
     * @param usuarioId       ID del colaborador al que se asignaran los prospectos
     * @param administradorId ID del administrador que realiza la asignacion
     * @param cantidad        Cantidad de prospectos a asignar (null = todos los disponibles)
     * @return Map con el resultado de la operacion
     */
    @Transactional
    public Map<String, Object> asignarCargaMasivaAUsuario(Long cargaMasivaId, Long usuarioId,
                                                           Long administradorId, Integer cantidad) {
        CargaMasiva cargaMasiva = cargaMasivaRepository.findById(cargaMasivaId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Carga masiva no encontrada con ID: " + cargaMasivaId));

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Usuario no encontrado con ID: " + usuarioId));

        Usuario administrador = usuarioRepository.findById(administradorId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Administrador no encontrado con ID: " + administradorId));

        // BK-2: filtrar prospectos por el banco del colaborador destino.
        // Si el usuario tiene banco asignado, solo se consideran prospectos de ese banco;
        // si no tiene banco (caso administrador), se usa el pool completo.
        List<Prospecto> prospectosSinAsignar =
                usuario.getBanco() != null
                        ? prospectoRepository.findUnassignedByCargaMasivaAndBanco(
                                cargaMasiva, usuario.getBanco())
                        : prospectoRepository.findUnassignedByCargaMasiva(cargaMasiva);

        if (prospectosSinAsignar.isEmpty()) {
            String mensajeBanco = usuario.getBanco() != null
                    ? " de tu banco (" + usuario.getBanco().getNombre() + ")"
                    : "";
            throw new IllegalArgumentException(
                    "No hay prospectos sin asignar" + mensajeBanco + " en esta carga masiva.");
        }

        if (cantidad != null) {
            if (cantidad <= 0) {
                throw new IllegalArgumentException("La cantidad debe ser mayor a 0");
            }
            if (cantidad > prospectosSinAsignar.size()) {
                throw new IllegalArgumentException(
                        "La cantidad solicitada (" + cantidad + ") excede los prospectos disponibles ("
                        + prospectosSinAsignar.size() + ")");
            }
        }

        List<Prospecto> prospectosAAsignar = cantidad != null
                ? prospectosSinAsignar.stream().limit(cantidad).toList()
                : prospectosSinAsignar;

        LocalDateTime ahora = LocalDateTime.now();

        List<Asignacion> nuevasAsignaciones = prospectosAAsignar.stream()
                .map(prospecto -> {
                    Asignacion asignacion = new Asignacion();
                    asignacion.setProspecto(prospecto);
                    asignacion.setUsuario(usuario);
                    asignacion.setAdministrador(administrador);
                    asignacion.setAsignadoPor(administrador);
                    asignacion.setFechaAsignacion(ahora);
                    asignacion.setFechaAsignacionRegistro(ahora);
                    // Estado inicial canonico — sin literales de texto
                    asignacion.setEstado(EstadoGestion.SIN_GESTIONAR);
                    return asignacion;
                })
                .toList();

        if (!nuevasAsignaciones.isEmpty()) {
            asignacionRepository.saveAll(nuevasAsignaciones);
        }

        cargaMasivaService.actualizarEstadoAsignacion(cargaMasivaId);

        int restantes = prospectosSinAsignar.size() - prospectosAAsignar.size();

        Map<String, Object> resultado = new HashMap<>();
        resultado.put("success", true);
        resultado.put("mensaje", "Asignacion completada exitosamente");
        resultado.put("cargaMasivaId", cargaMasivaId);
        resultado.put("cargaMasivaNombre", cargaMasiva.getNombrearchivo());
        resultado.put("usuarioId", usuarioId);
        resultado.put("usuarioNombre", usuario.getNombre() + " " + usuario.getApellidos());
        resultado.put("totalProspectos", prospectosSinAsignar.size());
        resultado.put("nuevasAsignaciones", nuevasAsignaciones.size());
        resultado.put("prospectosSinAsignar", restantes);
        resultado.put("fechaAsignacion", ahora);
        return resultado;
    }

    /**
     * Reparte prospectos de una carga a VARIOS colaboradores en un solo flujo (RF-19).
     * Solo cantidad exacta. La suma de cantidades no puede exceder los prospectos
     * sin asignar. Transaccional: o se asigna todo el reparto o nada.
     *
     * @return resumen con disponibles, asignados por usuario y saldo restante
     */
    @Transactional
    public Map<String, Object> asignarCargaMasivaMulti(
            Long cargaMasivaId,
            List<AsignacionMultiRequest.Item> items,
            Long administradorId) {

        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Debe indicar al menos un colaborador y cantidad.");
        }

        CargaMasiva cargaMasiva = cargaMasivaRepository.findById(cargaMasivaId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Carga masiva no encontrada con ID: " + cargaMasivaId));

        Usuario administrador = usuarioRepository.findById(administradorId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Administrador no encontrado con ID: " + administradorId));

        // Validar cada item y resolver usuarios (no se permiten usuarios repetidos)
        List<Long> vistos = new ArrayList<>();
        List<Usuario> usuarios = new ArrayList<>();
        int sumaSolicitada = 0;
        for (AsignacionMultiRequest.Item item : items) {
            if (item.getUsuarioId() == null || item.getCantidad() == null) {
                throw new IllegalArgumentException("Cada asignación requiere usuarioId y cantidad.");
            }
            if (item.getCantidad() <= 0) {
                throw new IllegalArgumentException("La cantidad debe ser mayor a 0.");
            }
            if (vistos.contains(item.getUsuarioId())) {
                throw new IllegalArgumentException(
                        "El colaborador " + item.getUsuarioId() + " está repetido en el reparto.");
            }
            vistos.add(item.getUsuarioId());
            Usuario u = usuarioRepository.findById(item.getUsuarioId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Usuario no encontrado con ID: " + item.getUsuarioId()));
            if (Boolean.FALSE.equals(u.getEstado())) {
                throw new IllegalArgumentException(
                        "El colaborador " + u.getUsuario() + " está inactivo.");
            }
            usuarios.add(u);
            sumaSolicitada += item.getCantidad();
        }

        // BK-2: cada colaborador solo recibe prospectos de su banco.
        // Pool total sin asignar (para el resumen final); el reparto efectivo
        // se hace por banco de cada colaborador.
        List<Prospecto> disponiblesTotal =
                prospectoRepository.findUnassignedByCargaMasiva(cargaMasiva);
        if (disponiblesTotal.isEmpty()) {
            throw new IllegalArgumentException(
                    "No hay prospectos disponibles para asignar en esta carga.");
        }

        LocalDateTime ahora = LocalDateTime.now();
        List<Asignacion> nuevas = new ArrayList<>();
        List<Map<String, Object>> detallePorUsuario = new ArrayList<>();
        // IDs ya comprometidos en esta transacción (para evitar solapamiento entre
        // colaboradores del mismo banco dentro del mismo reparto).
        java.util.Set<Long> yaComprometidos = new java.util.HashSet<>();

        for (int idx = 0; idx < items.size(); idx++) {
            AsignacionMultiRequest.Item item = items.get(idx);
            Usuario usuario = usuarios.get(idx);

            // Pool de prospectos sin asignar para el banco de este colaborador
            List<Prospecto> poolUsuario;
            if (usuario.getBanco() != null) {
                poolUsuario = prospectoRepository
                        .findUnassignedByCargaMasivaAndBanco(cargaMasiva, usuario.getBanco())
                        .stream()
                        .filter(p -> !yaComprometidos.contains(p.getProspectoID()))
                        .collect(java.util.stream.Collectors.toList());
            } else {
                // Sin banco asignado (admin): accede al pool global
                poolUsuario = disponiblesTotal.stream()
                        .filter(p -> !yaComprometidos.contains(p.getProspectoID()))
                        .collect(java.util.stream.Collectors.toList());
            }

            if (item.getCantidad() > poolUsuario.size()) {
                String bancoDes = usuario.getBanco() != null
                        ? " del banco " + usuario.getBanco().getNombre()
                        : "";
                throw new IllegalArgumentException(
                        "La cantidad solicitada (" + item.getCantidad() + ") para el colaborador "
                        + usuario.getUsuario() + " excede los prospectos disponibles"
                        + bancoDes + " (" + poolUsuario.size() + ").");
            }

            List<Prospecto> lote = poolUsuario.subList(0, item.getCantidad());
            for (Prospecto p : lote) {
                yaComprometidos.add(p.getProspectoID());
                Asignacion a = new Asignacion();
                a.setProspecto(p);
                a.setUsuario(usuario);
                a.setAdministrador(administrador);
                a.setAsignadoPor(administrador);
                a.setFechaAsignacion(ahora);
                a.setFechaAsignacionRegistro(ahora);
                a.setEstado(EstadoGestion.SIN_GESTIONAR);
                nuevas.add(a);
            }

            Map<String, Object> d = new HashMap<>();
            d.put("usuarioId", usuario.getId());
            d.put("usuarioNombre", usuario.getNombre() + " " + usuario.getApellidos());
            d.put("asignados", item.getCantidad());
            detallePorUsuario.add(d);
        }

        asignacionRepository.saveAll(nuevas);
        cargaMasivaService.actualizarEstadoAsignacion(cargaMasivaId);

        Map<String, Object> resultado = new HashMap<>();
        resultado.put("success", true);
        resultado.put("mensaje", "Reparto completado exitosamente");
        resultado.put("cargaMasivaId", cargaMasivaId);
        resultado.put("cargaMasivaNombre", cargaMasiva.getNombrearchivo());
        resultado.put("disponiblesAntes", disponiblesTotal.size());
        resultado.put("totalAsignados", nuevas.size());
        resultado.put("saldoSinAsignar", disponiblesTotal.size() - nuevas.size());
        resultado.put("detalle", detallePorUsuario);
        resultado.put("fechaAsignacion", ahora);
        return resultado;
    }

    /**
     * Obtiene estadisticas generales de asignaciones usando los enums canonicos.
     *
     * @return Map con las estadisticas
     */
    public Map<String, Object> obtenerEstadisticas() {
        Map<String, Object> estadisticas = new HashMap<>();

        long totalAsignaciones = asignacionRepository.count();
        long sinGestionar = asignacionRepository.countByEstado(EstadoGestion.SIN_GESTIONAR);
        long enGestion = asignacionRepository.countByEstado(EstadoGestion.EN_GESTION);
        long enSeguimiento = asignacionRepository.countByEstado(EstadoGestion.EN_SEGUIMIENTO);
        long derivados = asignacionRepository.countByEstado(EstadoGestion.DERIVADO);
        long ganados = asignacionRepository.countByEstado(EstadoGestion.GANADO);
        long descartados = asignacionRepository.countByEstado(EstadoGestion.DESCARTADO);
        long totalProspectos = prospectoRepository.count();
        long totalCargasMasivas = cargaMasivaRepository.count();
        long totalUsuarios = usuarioRepository.count();

        estadisticas.put("totalAsignaciones", totalAsignaciones);
        estadisticas.put("sinGestionar", sinGestionar);
        estadisticas.put("enGestion", enGestion);
        estadisticas.put("enSeguimiento", enSeguimiento);
        estadisticas.put("derivados", derivados);
        estadisticas.put("ganados", ganados);
        estadisticas.put("descartados", descartados);
        estadisticas.put("totalProspectos", totalProspectos);
        estadisticas.put("totalCargasMasivas", totalCargasMasivas);
        estadisticas.put("totalUsuarios", totalUsuarios);
        estadisticas.put("porcentajeAsignado",
                totalProspectos > 0 ? (double) totalAsignaciones / totalProspectos * 100 : 0);

        return estadisticas;
    }
}
