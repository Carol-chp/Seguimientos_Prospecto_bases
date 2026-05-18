---
name: wizard-atencion-rf04-slice13
description: Slice 1.3 — Wizard de atencion (RF-04/13/14/15/16): apertura modal, verificacion SBS, registrar atencion, historial prospecto
metadata:
  type: project
---

## Slice 1.3 — Wizard de atencion completado (RF-04/13/14/15/16)

### Nuevos archivos creados
- `Repository/ConfiguracionDuenoRepository.java` — findTopByOrderByIdAsc() para singleton
- `Repository/AperturaEventoRepository.java` — JpaRepository base (suficiente)
- `dto/AperturaResponseDTO.java` — { aperturaId, inicio }
- `dto/VerificacionSbsRequestDTO.java` — { prospectoId, resultado, fechaReevaluacion, comentario }
- `dto/HistorialContactoDTO.java` — { fechaContacto, resultado, submotivoNoContesto, quienContesto, verificacionSbs, comentario, duracionGestion }

### Archivos modificados
- `dto/ContactoRegistroDTO.java` — agrega: aperturaId, resultado, submotivoNoContesto, quienContesto, duracionGestionSegundos; alias legacy estadoResultado mantenido
- `Repository/ContactoRepository.java` — agrega findHistorialByProspectoId(@Param Long) con JPQL join a asignacion.prospecto
- `Service/ContactoService.java` — reescrito completo con 4 operaciones + helpers privados
- `Controller/ContactoController.java` — reescrito con 5 endpoints + obtenerUsuarioAutenticado()

### Endpoints expuestos

| Metodo | URL | Descripcion |
|--------|-----|-------------|
| POST | /api/contactos/apertura | Crea AperturaEvento, responde { aperturaId, inicio } |
| POST | /api/contactos/apertura/{id}/cerrar | Cierra modal sin registro (idempotente) |
| POST | /api/contactos/verificacion-sbs | Paso 0 SBS; APTO→{continuar:true}, OBSERVADO→{continuar:false, estado, fechaReevaluacionSbs} |
| POST | /api/contactos | Registrar atencion; responde { ok, estado, proximaLlamada } |
| GET | /api/contactos/historial/{prospectoId} | Historial todos los ciclos, orden DESC |

### Reglas de negocio clave
- SBS bloquea: verificacionSbs != APTO → 400 al intentar registrar llamada
- NO_CONTESTO: intentosFallidos++ ; si > ConfiguracionDueno.maxIntentosNoContesto → DESCARTADO/ILOCALIZABLE
- Regla de reintento: "+3h,+24h,+48h,+72h,+120h" CSV; indice = intentos-1, ultimo si excede
- OBSERVADO SBS: fechaAgenda = fechaReevaluacion @ 09:00 para activacion por cola D2
- resultado=ILOCALIZABLE lanzado desde wizard → 400 (es estado interno calculado)
- aperturaId opcional: si viene, se cierra el AperturaEvento (fin=now, huboRegistro=true) y se calcula duracion
- ConfiguracionDueno singleton: findTopByOrderByIdAsc(); si no existe, new ConfiguracionDueno() con defaults

### Smoke test resultados (2026-05-17)
- A: POST /api/contactos sin SBS → 400 OK
- B: POST /api/contactos/apertura → { aperturaId:1, inicio:ISO } OK
- C: POST verificacion-sbs APTO → { continuar:true } OK
- D: NO_CONTESTO x7 (max=6) → intentos 1-6 EN_SEGUIMIENTO, intento 7 DESCARTADO OK
- E: verificacion-sbs OBSERVADO → { continuar:false, estado:EN_SEGUIMIENTO, fechaReevaluacionSbs:+90d } OK
- F: GET historial/1 → 7 registros; historial/2 → 1 registro (SBS OBSERVADO) OK

**Why:** Slice 1.3 del plan de desarrollo incremental. El wizard es el flujo central del teleoperador.
**How to apply:** Slice 1.4 (derivacion+cierre) puede reutilizar obtenerUsuarioAutenticado() del controller y el patron resolverCicloActivo() del service.
