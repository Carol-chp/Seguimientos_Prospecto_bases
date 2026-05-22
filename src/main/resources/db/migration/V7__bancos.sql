-- ============================================================
-- V7__bancos.sql
-- Catálogo de Bancos + relación banco ↔ usuario/prospecto.
--
-- Diseño:
--   - banco (id, nombre, activo, es_default, banco_destino_id self-FK)
--   - usuario.banco_id  → FK a banco (nullable; admins quedan null)
--   - prospecto.banco_id → FK a banco (todos los existentes → Scotiabank)
--
-- Seed: Scotiabank (es_default=true) y BBVA.
--       Scotiabank.banco_destino_id → BBVA (bases OBSERVADO de Scotiabank van a BBVA).
-- ============================================================

CREATE TABLE banco (
    id               BIGSERIAL    NOT NULL,
    nombre           VARCHAR(80)  NOT NULL,
    activo           BOOLEAN      NOT NULL DEFAULT TRUE,
    es_default       BOOLEAN      NOT NULL DEFAULT FALSE,
    banco_destino_id BIGINT,
    CONSTRAINT banco_pkey        PRIMARY KEY (id),
    CONSTRAINT banco_nombre_uq   UNIQUE (nombre),
    CONSTRAINT banco_destino_fk  FOREIGN KEY (banco_destino_id) REFERENCES banco (id)
);

-- Seed de bancos
INSERT INTO banco (nombre, activo, es_default) VALUES ('Scotiabank', TRUE, TRUE);
INSERT INTO banco (nombre, activo, es_default) VALUES ('BBVA',       TRUE, FALSE);

-- Scotiabank envía sus OBSERVADO a BBVA
UPDATE banco
SET banco_destino_id = (SELECT id FROM banco WHERE nombre = 'BBVA')
WHERE nombre = 'Scotiabank';

-- Resincronizar la secuencia BIGSERIAL de banco tras los inserts con id implícito
SELECT setval(pg_get_serial_sequence('banco', 'id'),
              (SELECT MAX(id) FROM banco));

-- ============================================================
-- Añadir banco_id a usuario
-- ============================================================
ALTER TABLE usuario
    ADD COLUMN banco_id BIGINT,
    ADD CONSTRAINT usuario_banco_fk FOREIGN KEY (banco_id) REFERENCES banco (id);

-- Los TELEOPERADOR existentes van a Scotiabank; admins quedan null
UPDATE usuario
SET banco_id = (SELECT id FROM banco WHERE nombre = 'Scotiabank')
WHERE idrol = (SELECT id FROM rol WHERE nombre = 'TELEOPERADOR');

-- ============================================================
-- Añadir banco_id a prospecto
-- ============================================================
ALTER TABLE prospecto
    ADD COLUMN banco_id BIGINT,
    ADD CONSTRAINT prospecto_banco_fk FOREIGN KEY (banco_id) REFERENCES banco (id);

-- Todos los prospectos existentes van al banco default (Scotiabank)
UPDATE prospecto
SET banco_id = (SELECT id FROM banco WHERE nombre = 'Scotiabank');
