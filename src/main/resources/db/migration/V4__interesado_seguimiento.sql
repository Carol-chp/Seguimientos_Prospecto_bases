-- V4: parámetro de seguimiento para INTERESADO.
-- Días hábiles por defecto para el recordatorio cuando se marca "Interesado"
-- (el caso pasa a EN_SEGUIMIENTO con fechaAgenda sugerida, editable).
ALTER TABLE configuracion_dueno
    ADD COLUMN plazo_seguimiento_interesado_dias INTEGER NOT NULL DEFAULT 1;
