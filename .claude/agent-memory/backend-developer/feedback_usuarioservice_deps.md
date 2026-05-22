---
name: feedback-usuarioservice-deps
description: UsuarioService constructor dependency order — important for @InjectMocks to work correctly in tests
metadata:
  type: feedback
---

`UsuarioService` (as of BK-1) uses `@RequiredArgsConstructor`, injecting 5 deps in this order:

1. `UsuarioRepository`
2. `RolRepository`
3. `AsignacionRepository`
4. `BancoRepository`
5. `PasswordEncoder`

**Why:** `@InjectMocks` in Mockito injects by constructor. All 5 must be declared as `@Mock` in `UsuarioServiceTest` or injection silently fails.

**How to apply:** Whenever adding a new dependency to `UsuarioService`, add the corresponding `@Mock` in `UsuarioServiceTest`. The original test was missing `@Mock AsignacionRepository` (Mockito 5 tolerates this with lenient injection, so tests passed — but best to have all mocks explicit).
