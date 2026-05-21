-- ============================================================
-- V6__email_reportes.sql
-- Agrega email_reportes a configuracion_dueno.
-- Este campo almacena el destinatario del resumen diario y las
-- notificaciones de atención, desacoplándolo del correo del usuario
-- administrador (que pasa a ser opcional en el formulario de usuarios).
-- ============================================================

ALTER TABLE configuracion_dueno
    ADD COLUMN email_reportes VARCHAR(150);

-- Sembrar con el correo del primer administrador activo (si existe).
-- Si no hay admin con correo, queda null → EmailService cae al fallback.
UPDATE configuracion_dueno
SET email_reportes = (
    SELECT u.correo
    FROM usuario u
    JOIN rol r ON r.id = u.idrol
    WHERE r.nombre = 'ADMINISTRADOR'
      AND u.estado = TRUE
      AND u.correo IS NOT NULL
      AND u.correo <> ''
    ORDER BY u.id
    LIMIT 1
)
WHERE email_reportes IS NULL;
