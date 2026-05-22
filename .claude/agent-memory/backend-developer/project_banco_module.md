---
name: project-banco-module
description: BK-1 — Banco entity, V7 migration, banco FK in Usuario and Prospecto, BancoService, BancoController
metadata:
  type: project
---

Implemented as part of task BK-1 (backend banco module).

## Files created
- `Entity/Banco.java` — tabla `banco`, self-FK `banco_destino_id`
- `Repository/BancoRepository.java` — JpaRepository + findFirstByEsDefaultTrue() + findByActivoTrueOrderByNombreAsc()
- `db/migration/V7__bancos.sql` — CREATE TABLE banco, seed Scotiabank (default) + BBVA, ALTER TABLE usuario/prospecto ADD COLUMN banco_id
- `dto/BancoRequestDTO.java` — payload create/update (nombre, activo, esDefault, bancoDestinoId)
- `dto/BancoResponseDTO.java` — response with bancoDestinoId/bancoDestinoNombre flattened (no circular JSON)
- `Service/BancoService.java` — listarActivos, obtenerPorId, crear, actualizar
- `Controller/BancoController.java` — GET /api/bancos (autenticado), GET /api/bancos/{id}, POST, PUT (admin)

## Files modified
- `Entity/Usuario.java` — added `@ManyToOne Banco banco`
- `Entity/Prospecto.java` — added `@ManyToOne Banco bancoEntidad` (field named bancoEntidad to avoid collision with existing String banco field)
- `Service/UsuarioService.java` — injected BancoRepository, resolverBanco() helper, bancoId/bancoNombre in convertirADTO
- `Service/ProspectoService.java` — injected BancoRepository, sets bancoEntidad=bancoDefault on each imported prospecto
- `dto/UsuarioDTO.java` — added bancoId (Long) + bancoNombre (String) getters/setters
- `dto/CreateUsuarioRequestDTO.java` — added bancoId field (optional)
- `dto/UpdateUsuarioRequestDTO.java` — added bancoId field (optional)
- `test/UsuarioServiceTest.java` — added @Mock AsignacionRepository + @Mock BancoRepository; updated all DTO constructor calls to 7 args

## Key design decision
`Prospecto` already had a `String banco` field (column `banco`). The new ManyToOne FK was named `bancoEntidad` with `@JoinColumn(name="banco_id")` to avoid any column naming collision.

**Why:** bancoId=null is valid for admins; resolverBanco(null) returns null without hitting the DB, so existing tests that pass bancoId=null compile and pass without additional stubs.
