---
name: feedback-create-usuario-constructor
description: CreateUsuarioRequestDTO @AllArgsConstructor signature after BK-1
metadata:
  type: feedback
---

`CreateUsuarioRequestDTO` uses `@Data @AllArgsConstructor`, so Lombok generates a constructor with ALL fields in declaration order. After BK-1 there are 7 params:

```
new CreateUsuarioRequestDTO(nombre, apellidos, usuario, email, password, rolId, bancoId)
```

**Why:** Added `bancoId` as the 7th field. Tests using the constructor must be updated when this DTO gains new fields.

**How to apply:** Any test or code that calls `new CreateUsuarioRequestDTO(...)` with positional args must pass `null` for bancoId when no banco is needed.
