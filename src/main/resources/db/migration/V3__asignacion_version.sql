-- V3: Add optimistic locking version column to asignacion table.
-- Existing rows start at version 0 so the first UPDATE does not conflict.
ALTER TABLE asignacion ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
