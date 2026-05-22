---
name: feedback-update-usuario-constructor
description: UpdateUsuarioRequestDTO @AllArgsConstructor signature after BK-1
metadata:
  type: feedback
---

`UpdateUsuarioRequestDTO` uses `@Data @AllArgsConstructor`, so Lombok generates a constructor with ALL fields in declaration order. After BK-1 there are 7 params:

```
new UpdateUsuarioRequestDTO(nombre, apellidos, email, password, rolId, estado, bancoId)
```

**Why:** Added `bancoId` as the 7th field. Tests using the constructor must be updated when this DTO gains new fields.

**How to apply:** Any test or code that calls `new UpdateUsuarioRequestDTO(...)` with positional args must be updated when fields change.
