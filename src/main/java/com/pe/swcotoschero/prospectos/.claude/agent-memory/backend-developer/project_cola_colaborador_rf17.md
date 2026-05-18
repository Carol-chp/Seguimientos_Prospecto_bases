---
name: cola-colaborador-rf17
description: Slice 1.2 — cola del colaborador (RF-17): 11 filtros, enmascarado de datos sensibles, endpoint mi-actividad
metadata:
  type: project
---

Slice 1.2 completado (2026-05-17). Endpoint GET /api/asignaciones/mis-prospectos ahora
acepta parámetro `filtro` (FiltroColaborador enum, 11 valores) + `busqueda` + pagina/tamanioPagina.
El usuario autenticado se resuelve internamente; el frontend no lo pasa.

**Why:** RF-17 requiere que el colaborador vea su cola filtrada por 11 vistas distintas
(MI_COLA_HOY default, SIN_GESTIONAR, AGENDADOS_HOY, POR_REINTENTAR, PROGRAMADOS,
OBSERVADO_SBS, DERIVADOS, INTERESADOS, MIS_VENTAS, DESCARTADOS, TODOS).

**How to apply:**
- Zona horaria: America/Lima siempre (ZoneId.of("America/Lima") en ColaboradorColaService).
- Enmascarado: celular y documentoIdentidad → solo últimos 3 chars visibles, prefijo "*".
  Método estático: ColaboradorColaService.enmascararSensible(). La búsqueda opera sobre
  el valor real (la query JPQL compara con el campo real del prospecto, no el enmascarado).
- Banderas vencido/futuro: solo aplican a EN_SEGUIMIENTO y se calculan en Java (no en JPQL).
- Queries JPQL: una por filtro en AsignacionRepository (findFiltroXxx). Spring Data
  valida todas en startup — si una query falla, el app no levanta (detecta errores temprano).
- Actividad del día: GET /api/asignaciones/mi-actividad → MiActividadDTO (ContactoRepository.findActividadDelDia).
- Parámetros legacy estado/estadoResultado en mis-prospectos se aceptan pero se ignoran
  (compatibilidad hacia atrás).
- Filtro inválido → IllegalArgumentException → GlobalExceptionHandler → HTTP 400 con
  lista de valores permitidos.
- ColaboradorColaService usa constructor injection (no @Autowired en campo) — patrón
  adoptado en el controller también.

**Archivos cambiados/creados:**
- NUEVO: Entity/enums/FiltroColaborador.java
- NUEVO: dto/MiActividadDTO.java
- NUEVO: Service/ColaboradorColaService.java
- MODIFICADO: dto/MiProspectoDTO.java (campos nuevos + asignacionId)
- MODIFICADO: Repository/AsignacionRepository.java (11 queries findFiltroXxx)
- MODIFICADO: Repository/ContactoRepository.java (findActividadDelDia)
- MODIFICADO: Controller/AsignacionController.java (reescrito con constructor injection)
