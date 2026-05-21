---
name: cierre-colaborador-cb1-cb2
description: Colaborador cierra directamente en GANADO (no pasa por dueño); CierreController/Service eliminados; email a Configuración; export solo admin; labels Convenio
type: project
---

## Decisiones de la feature CB-1 + CB-2 (implementada 2026-05-21)

El colaborador marca "Derivar (ACEPTÓ)" → `ContactoService` case DERIVADO transiciona directamente a `GANADO` (no a `DERIVADO`). `derivadoPor` + `cerradoPor` = colaborador autenticado. `fechaElegibilidad` queda null para que el job D7 no reprocese.

`CierreController`, `CierreService`, y DTOs exclusivos `CierreVentaRequest`, `NoCerroRequest`, `PorCerrarItemDTO` eliminados del proyecto.

`findPorCerrar` eliminado de `AsignacionRepository`. `countDerivadosGlobal`, `countDerivadosPeriodo`, `countDerivadosColaboradorPeriodo` **se mantienen** porque los usa `calcularEmbudo()` y el ranking (historial de eventos DERIVADO via `fechaDerivacion`).

`DashboardDTO` eliminó campo `porCerrar`. `ReportesService.calcularDashboard()` no llama `countDerivadosGlobal` para `porCerrar` — ese bloque fue quitado.

`tasaConversion` recalculada: `ventasMes / contactadosTitularMes` (denominador con sentido real; guarda división por cero → 0.0). Antes era `ventasMes / derivadosMes`.

**Why:** `derivadosMes` ya no es intermediario útil (el colaborador cierra directo), entonces se usa `contactadosTitular` como base de conversión real.

**V6__email_reportes.sql:** `ALTER TABLE configuracion_dueno ADD COLUMN email_reportes VARCHAR(150)` + seed desde correo del primer ADMINISTRADOR activo.

`ConfiguracionDueno.emailReportes` + `ConfiguracionRequest.emailReportes` + patch en `ConfiguracionController` (validación regex básica si no null/blank).

`EmailService.resolverDestinatario(cfg)`: prioridad → `cfg.emailReportes`; fallback → correo del admin. Log warn si ambos vacíos.

Email del usuario (`CreateUsuarioRequestDTO`, `UpdateUsuarioRequestDTO`) pasó a **opcional** (quitó `@NotBlank`). `UsuarioService` maneja null/blank → guarda null.

`exportar-mis-prospectos` → `@PreAuthorize("hasRole('ADMINISTRADOR')")`.

Labels Excel "Campaña" → "Convenio" en `ReportesService` (2 headers) y `BitacoraService` (1 header). HTML de correos: eliminado bloque "Por cerrar".

**How to apply:** Al implementar nuevas features de cierre/venta verificar que GANADO es el único estado terminal de venta; DERIVADO ahora solo existe como `estadoResultado` histórico (nunca como `estadoGestion` activo).
