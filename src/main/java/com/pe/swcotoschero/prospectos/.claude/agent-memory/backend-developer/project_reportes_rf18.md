---
name: project-reportes-rf18
description: Slice 1.5 ReportesController + dashboard del dueño MVP (RF-18, §5e/§5f) — 4 endpoints implementados
metadata:
  type: project
---

# Slice 1.5 — Reportes y métricas del dueño (RF-18)

Implementado en 2026-05-17.

## Archivos creados
- `Controller/ReportesController.java` — 4 endpoints bajo `/api/reportes/*`
- `Service/ReportesService.java` — toda la lógica de métricas, Excel
- `dto/reporte/DashboardDTO.java`, `MetricasPeriodoDTO.java`, `RankingColaboradorDTO.java`, `EmbudoDTO.java`, `BaseResumenDTO.java`, `DrillDownAsignacionDTO.java`

## Archivos modificados
- `Repository/ContactoRepository.java` — agregadas queries de reportes
- `Repository/AsignacionRepository.java` — agregadas queries de reportes (embudo, ventas, derivados por período, exportación)

## Endpoints
1. `GET /api/reportes/dashboard` — solo ADMINISTRADOR. JSON exacto con dia/mes/ranking/embudo/porCerrar/bases.
2. `GET /api/reportes/colaborador/{usuarioId}?pagina=1&tamanioPagina=10` — solo ADMINISTRADOR. Drill-down paginado.
3. `GET /api/reportes/exportar-prospectos?campania=&estado=&estadoResultado=` — solo ADMINISTRADOR. Descarga .xlsx sin enmascarar.
4. `GET /api/reportes/exportar-mis-prospectos?filtro=TODOS` — cualquier autenticado. Descarga .xlsx enmascarado (delega a ColaboradorColaService).

## Bug encontrado y corregido
`ContactoRepository.contactabilidadGlobal()` y `contactabilidadColaborador()` tenían return type `Object[]`. Hibernate con un resultado único y múltiples expresiones SUM devolvía un `Object[]` de longitud 1 (unwrap). **Solución:** cambiar return type a `List<Object[]>` y acceder via `.get(0)`.

**Why:** Con `Object[]` Spring Data JPA puede "desenvolver" el único resultado cuando hay una sola fila, quedando el array de longitud 1 en vez de 2.

## Supuestos
- `ADMIN_ROL_ID = 1L` (hardcoded en ReportesService; alinea con el esquema existente)
- avanceBasesPct = (asignaciones con estado != SIN_GESTIONAR) / total_asignaciones (todos los prospectos alguna vez asignados)
- Export mis-prospectos limita a 5000 registros (protección memoria)
- Periodo "mes" = desde día 1 del mes actual hasta instante actual (no fin de mes)

## Pendiente para Fase 2
- §5e "colaboradores ausentes" / "en riesgo" — no implementado (Fase 2)
- Exportación con más filtros (campania por ID, rango de fechas)

**How to apply:** El email resumen (RF-11/slice 1.6) puede reutilizar `ReportesService.calcularDashboard()` directamente — ya produce todas las métricas necesarias.
