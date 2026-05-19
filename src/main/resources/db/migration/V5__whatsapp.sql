-- V5: plantilla de WhatsApp (configurable por el dueño) + tarjeta de firma por colaborador.

ALTER TABLE configuracion_dueno ADD COLUMN plantilla_whatsapp TEXT;

UPDATE configuracion_dueno SET plantilla_whatsapp =
'Estimad@ {nombre}, reciba un cordial saludo de parte del BANCO SCOTIABANK.

Le recordamos que mantenemos activo nuestro convenio con entidades públicas.

*Tiene un préstamo previa evaluación de hasta S/ 200 mil soles (previa evaluación de boleta de pago).
*Compramos deudas de todos los bancos y tarjetas de crédito.

Beneficios:
*La tasa más baja del convenio.
*Descuento directo de su boleta.

NO ESPERES MÁS Y SOLICITA MAYOR INFORMACIÓN CON TU ASESOR.

Saludos cordiales,
{asesor}'
WHERE plantilla_whatsapp IS NULL;

-- Tarjeta de firma por colaborador (tabla aparte para no engordar la carga de Usuario).
CREATE TABLE usuario_tarjeta_whatsapp (
    usuario_id  BIGINT      PRIMARY KEY REFERENCES usuario(id) ON DELETE CASCADE,
    imagen      BYTEA       NOT NULL,
    tipo        VARCHAR(64) NOT NULL,
    actualizado TIMESTAMP   NOT NULL DEFAULT now()
);
