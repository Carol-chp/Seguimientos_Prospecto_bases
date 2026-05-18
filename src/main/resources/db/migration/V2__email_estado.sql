-- ============================================================
-- V2__email_estado.sql
-- Estado del último envío del resumen diario (RF-07).
-- Permite "avisar en el dashboard" si el correo falló (registro + aviso).
-- ============================================================

ALTER TABLE configuracion_dueno
    ADD COLUMN ultimo_envio_resumen_ok      BOOLEAN,
    ADD COLUMN ultimo_envio_resumen_fecha   TIMESTAMP,
    ADD COLUMN ultimo_envio_resumen_detalle VARCHAR(500);
