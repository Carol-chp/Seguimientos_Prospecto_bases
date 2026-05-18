-- ============================================================
-- V1__baseline.sql
-- Esquema completo desde cero para el Sistema de Seguimiento de Prospectos.
-- Negocio nuevo sin datos productivos: se recrea todo el esquema.
-- NO ejecutar sobre una BD con datos reales sin respaldar primero.
--
-- Enums canonicos:
--   estado_gestion: DISPONIBLE|SIN_GESTIONAR|EN_GESTION|EN_SEGUIMIENTO|DERIVADO|GANADO|DESCARTADO
--   resultado_atencion: NO_CONTESTO|VOLVER_LLAMAR|AGENDADO|INTERESADO|DERIVADO|NO_VOLVER_LLAMAR|DATOS_INVALIDOS|ILOCALIZABLE
--   submotivo_no_contesto: NO_CONTESTA|BUZON|OCUPADO|APAGADO
--   quien_contesto: TITULAR|TERCERO|EQUIVOCADO
--   verificacion_sbs: APTO|OBSERVADO
-- ============================================================

-- ============================================================
-- Nota sobre enums: los enums se persisten como VARCHAR
-- (`@Enumerated(EnumType.STRING)` en JPA). NO se usan tipos ENUM de
-- PostgreSQL porque Hibernate los mapea a varchar/text; crear tipos
-- nativos solo añade fricción (casts) sin beneficio. Los valores
-- válidos están documentados en la cabecera y en los enums Java.
-- ============================================================

-- ============================================================
-- 2. Tablas de soporte (sin FK hacia tablas principales)
-- ============================================================

CREATE TABLE rol (
    id      BIGSERIAL    NOT NULL,
    nombre  VARCHAR(50)  NOT NULL,
    CONSTRAINT rol_pkey PRIMARY KEY (id),
    CONSTRAINT rol_nombre_uq UNIQUE (nombre)
);

CREATE TABLE usuario (
    id       BIGSERIAL    NOT NULL,
    nombre   VARCHAR(100) NOT NULL,
    apellidos VARCHAR(100),
    usuario  VARCHAR(50)  NOT NULL,
    correo   VARCHAR(150),
    password VARCHAR(255) NOT NULL,
    estado   BOOLEAN      NOT NULL DEFAULT TRUE,
    idrol    BIGINT       NOT NULL,
    CONSTRAINT usuario_pkey        PRIMARY KEY (id),
    CONSTRAINT usuario_usuario_uq  UNIQUE (usuario),
    CONSTRAINT usuario_rol_fk      FOREIGN KEY (idrol) REFERENCES rol (id)
);

CREATE TABLE campania (
    id          BIGSERIAL    NOT NULL,
    nombre      VARCHAR(100) NOT NULL,
    descripcion VARCHAR(500),
    CONSTRAINT campania_pkey PRIMARY KEY (id)
);

-- ============================================================
-- 3. Carga masiva (base/Excel)
-- ============================================================

CREATE TABLE cargamasiva (
    id                  BIGSERIAL    NOT NULL,
    nombrearchivo       VARCHAR(255) NOT NULL,
    fecha               TIMESTAMP,
    usuario_asignado_id BIGINT,
    cantidad_prospectos INTEGER      NOT NULL DEFAULT 0,
    -- SIN_ASIGNAR | PARCIALMENTE_ASIGNADO | ASIGNADO
    estado_asignacion   VARCHAR(30)  NOT NULL DEFAULT 'SIN_ASIGNAR',
    fecha_asignacion    TIMESTAMP,
    CONSTRAINT cargamasiva_pkey          PRIMARY KEY (id),
    CONSTRAINT cargamasiva_usuario_fk    FOREIGN KEY (usuario_asignado_id) REFERENCES usuario (id)
);

-- ============================================================
-- 4. Prospecto (persona — datos personales del lead)
-- ============================================================

CREATE TABLE prospecto (
    id                       BIGSERIAL    NOT NULL,
    nombre                   VARCHAR(100),
    apellido                 VARCHAR(100),
    celular                  VARCHAR(20),
    documentoidentidad       VARCHAR(20),
    sexo                     VARCHAR(10),
    banco                    VARCHAR(100),
    cargo                    VARCHAR(100),
    distrito                 VARCHAR(100),
    campania_id              BIGINT,
    idcargamasiva            BIGINT,
    subcampania              VARCHAR(100),
    estado_interesado        BOOLEAN      DEFAULT FALSE,
    -- Numero de prestamos concretados (ciclos GANADO).
    -- Alimenta el badge "Cliente recurrente" (RF-17e / D7.b).
    nro_prestamos_concretados INTEGER     NOT NULL DEFAULT 0,
    CONSTRAINT prospecto_pkey         PRIMARY KEY (id),
    CONSTRAINT prospecto_campania_fk  FOREIGN KEY (campania_id)  REFERENCES campania (id),
    CONSTRAINT prospecto_carga_fk     FOREIGN KEY (idcargamasiva) REFERENCES cargamasiva (id)
);

-- ============================================================
-- 5. Asignacion / Ciclo de gestion
--    El estado y los filtros operan sobre el CICLO, no el prospecto.
--    Un prospecto puede tener multiples ciclos: historicos GANADO + uno activo.
--    NO hay constraint de unicidad prospecto<->usuario (re-elegibilidad D7).
-- ============================================================

CREATE TABLE asignacion (
    id                       BIGSERIAL   NOT NULL,

    -- Relaciones principales
    prospecto_id             BIGINT      NOT NULL,
    personal_id              BIGINT      NOT NULL,   -- colaborador dueno del ciclo
    administrador_id         BIGINT      NOT NULL,   -- admin que creo la asignacion original

    -- Estado canonico del ciclo (enum)
    estado                   VARCHAR(20) NOT NULL DEFAULT 'SIN_GESTIONAR',
    estado_resultado         VARCHAR(20),            -- ResultadoAtencion de la ultima atencion

    -- Fechas base
    fecha_asignacion         TIMESTAMP   NOT NULL DEFAULT NOW(),
    -- Fecha/hora del proximo recontacto (fuente de verdad para la cola — D2).
    -- El estado NO cambia por job; la cola calcula accionable comparando esta fecha.
    fecha_agenda             TIMESTAMP,

    -- Verificacion SBS (RF-15)
    verificacion_sbs         VARCHAR(15),            -- VerificacionSbs: APTO | OBSERVADO
    fecha_consulta_sbs       TIMESTAMP,
    fecha_reevaluacion_sbs   DATE,

    -- Wizard de llamada (RF-04, RF-13, RF-16)
    quien_contesto           VARCHAR(15),            -- QuienContesto: TITULAR | TERCERO | EQUIVOCADO
    submotivo_no_contesto    VARCHAR(15),            -- SubmotivoNoContesto
    intentos_fallidos        INTEGER     NOT NULL DEFAULT 0,
    proxima_llamada          TIMESTAMP,
    -- Duracion en segundos del modal abierto->guardado (incluye SBS). "Tiempo de gestion".
    duracion_gestion         INTEGER,
    -- Marca interna del fin de SBS en el modal (no cambia UX; para split futuro D1).
    marca_fin_sbs            TIMESTAMP,

    -- Derivacion y cierre (5c.bis, D4)
    derivado_por_id          BIGINT,                 -- colaborador que derivo el caso
    fecha_derivacion         TIMESTAMP,
    cerrado_por_id           BIGINT,                 -- admin que registro la venta
    fecha_cierre             TIMESTAMP,

    -- Re-elegibilidad post-venta (D7)
    -- Fecha a partir de la cual el prospecto puede ser contactado de nuevo.
    -- Obligatoria al registrar GANADO. Al vencer, el job crea ciclo nuevo.
    fecha_elegibilidad       DATE,
    -- ID de la asignacion GANADO que origino este ciclo (cadena D7). Null = primer ciclo.
    ciclo_anterior_id        BIGINT,

    -- Auditoria de asignacion / reasignacion (5g, 5j)
    asignado_por_id          BIGINT,
    fecha_asignacion_registro TIMESTAMP,
    reasignado_de_id         BIGINT,                 -- ciclo activo del que proviene
    reasignado_para_id       BIGINT,                 -- ciclo nuevo al que se reasigno
    fecha_reasignacion       TIMESTAMP,
    motivo_reasignacion      VARCHAR(500),

    CONSTRAINT asignacion_pkey            PRIMARY KEY (id),
    CONSTRAINT asignacion_prospecto_fk    FOREIGN KEY (prospecto_id)   REFERENCES prospecto (id),
    CONSTRAINT asignacion_personal_fk     FOREIGN KEY (personal_id)    REFERENCES usuario (id),
    CONSTRAINT asignacion_admin_fk        FOREIGN KEY (administrador_id) REFERENCES usuario (id),
    CONSTRAINT asignacion_derivado_fk     FOREIGN KEY (derivado_por_id) REFERENCES usuario (id),
    CONSTRAINT asignacion_cerrado_fk      FOREIGN KEY (cerrado_por_id)  REFERENCES usuario (id),
    CONSTRAINT asignacion_asignado_por_fk FOREIGN KEY (asignado_por_id) REFERENCES usuario (id)
);

-- Indice para cola del colaborador (filtro por usuario + estado, consulta mas frecuente)
CREATE INDEX idx_asignacion_usuario_estado ON asignacion (personal_id, estado);
-- Indice para calcular la cola accionable por fecha (D2)
CREATE INDEX idx_asignacion_fecha_agenda  ON asignacion (personal_id, fecha_agenda)
    WHERE estado = 'EN_SEGUIMIENTO';
-- Indice para el job de re-elegibilidad (D7)
CREATE INDEX idx_asignacion_elegibilidad  ON asignacion (fecha_elegibilidad)
    WHERE estado = 'GANADO';

-- ============================================================
-- 6. Contacto (historial de atenciones)
-- ============================================================

CREATE TABLE contacto (
    id                  BIGSERIAL   NOT NULL,
    asignacion_id       BIGINT      NOT NULL,
    fecha_contacto      TIMESTAMP   NOT NULL DEFAULT NOW(),
    comentario          TEXT,

    -- Resultado canonico (wizard RF-04)
    estado_resultado         VARCHAR(20),   -- ResultadoAtencion
    submotivo_no_contesto    VARCHAR(15),   -- SubmotivoNoContesto
    quien_contesto           VARCHAR(15),   -- QuienContesto

    -- Verificacion SBS del Paso 0 (RF-15)
    verificacion_sbs         VARCHAR(15),   -- VerificacionSbs
    fecha_consulta_sbs       TIMESTAMP,

    -- Cronometro (RF-13)
    duracion_gestion         INTEGER,       -- segundos de gestion (incluye SBS)
    marca_fin_sbs            TIMESTAMP,     -- marca interna del fin de SBS

    -- Campos de compatibilidad hacia atras (deprecados)
    contesto_llamada         BOOLEAN,
    interesado               BOOLEAN,

    CONSTRAINT contacto_pkey        PRIMARY KEY (id),
    CONSTRAINT contacto_asig_fk     FOREIGN KEY (asignacion_id) REFERENCES asignacion (id)
);

CREATE INDEX idx_contacto_asignacion ON contacto (asignacion_id, fecha_contacto DESC);

-- ============================================================
-- 7. Apertura de evento (RF-14)
--    Cada apertura del modal registrada aunque no se guarde resultado.
-- ============================================================

CREATE TABLE apertura_evento (
    id            BIGSERIAL NOT NULL,
    asignacion_id BIGINT    NOT NULL,
    inicio        TIMESTAMP NOT NULL,
    fin           TIMESTAMP,
    -- true = termino en registro guardado; false/NULL = "abierto sin gestion"
    hubo_registro BOOLEAN   NOT NULL DEFAULT FALSE,
    CONSTRAINT apertura_evento_pkey    PRIMARY KEY (id),
    CONSTRAINT apertura_evento_asig_fk FOREIGN KEY (asignacion_id) REFERENCES asignacion (id)
);

CREATE INDEX idx_apertura_asignacion ON apertura_evento (asignacion_id);

-- ============================================================
-- 8. Jornada laboral (RF-21)
-- ============================================================

CREATE TABLE jornada (
    id         BIGSERIAL NOT NULL,
    usuario_id BIGINT    NOT NULL,
    fecha      DATE      NOT NULL,
    inicio     TIMESTAMP,
    fin        TIMESTAMP,
    CONSTRAINT jornada_pkey         PRIMARY KEY (id),
    CONSTRAINT jornada_usuario_fk   FOREIGN KEY (usuario_id) REFERENCES usuario (id),
    CONSTRAINT jornada_usuario_fecha_uq UNIQUE (usuario_id, fecha)
);

-- ============================================================
-- 9. Calendario laboral (RF-22)
--    Feriados nacionales de Peru precargados + editables por el dueno.
-- ============================================================

CREATE TABLE calendario_laboral (
    id          BIGSERIAL    NOT NULL,
    fecha       DATE         NOT NULL,
    es_feriado  BOOLEAN      NOT NULL DEFAULT TRUE,
    descripcion VARCHAR(200),
    CONSTRAINT calendario_laboral_pkey    PRIMARY KEY (id),
    CONSTRAINT calendario_laboral_fecha_uq UNIQUE (fecha)
);

-- ============================================================
-- 10. Configuracion del dueno (singleton — un solo registro)
--     Toggles de email, metas y parametros operativos.
-- ============================================================

CREATE TABLE configuracion_dueno (
    id                              BIGSERIAL    NOT NULL,

    -- Toggles de email (RF-06a / RF-06b / RF-07 / RF-08)
    toggle_email_instantaneo        BOOLEAN      NOT NULL DEFAULT FALSE,
    toggle_email_digest             BOOLEAN      NOT NULL DEFAULT FALSE,
    toggle_resumen_diario           BOOLEAN      NOT NULL DEFAULT FALSE,

    -- Metas configurables (5f). 0 = sin meta definida.
    meta_ventas_mensual             INTEGER      NOT NULL DEFAULT 0,
    meta_derivados_por_colaborador  INTEGER      NOT NULL DEFAULT 0,

    -- Parametros operativos (6b.1)
    plazo_reevaluacion_sbs_dias     INTEGER      NOT NULL DEFAULT 90,
    max_intentos_no_contesto        INTEGER      NOT NULL DEFAULT 6,
    -- Regla de reintento escalonada (horas) para NO_CONTESTO: "+3h,+24h,+48h,+72h,+120h"
    regla_reintento_no_contesto     VARCHAR(200) NOT NULL DEFAULT '+3h,+24h,+48h,+72h,+120h',
    hora_inicio_jornada             VARCHAR(5)   NOT NULL DEFAULT '09:00',
    minutos_gracia_ausencia         INTEGER      NOT NULL DEFAULT 45,

    CONSTRAINT configuracion_dueno_pkey PRIMARY KEY (id)
);

-- ============================================================
-- DATOS SEMILLA
-- ============================================================

-- Roles
INSERT INTO rol (id, nombre) VALUES (1, 'ADMINISTRADOR');
INSERT INTO rol (id, nombre) VALUES (2, 'TELEOPERADOR');

-- Usuario administrador (dueño) inicial — BOOTSTRAP.
-- Sin esto el sistema no tendría forma de iniciar sesión (se eliminó /register).
-- Credenciales por defecto:  usuario = admin   password = Admin123!
-- ⚠️ CAMBIAR la contraseña tras el primer login (gestión en /api/usuarios).
-- Hash BCrypt ($2b$10) de 'Admin123!' — compatible con BCryptPasswordEncoder.
INSERT INTO usuario (id, nombre, apellidos, usuario, correo, password, estado, idrol)
VALUES (1, 'Administrador', 'Sistema', 'admin', 'admin@local',
        '$2b$10$IouFAGbqLkQEStMqECjuRugGVx77rl9KAKpWWT2tfqJPXmo6ndUHm', TRUE, 1);

-- Resincronizar las secuencias BIGSERIAL tras inserts con id explícito,
-- para que el primer registro creado por la app (IDENTITY) no choque la PK.
SELECT setval(pg_get_serial_sequence('rol', 'id'),
              (SELECT MAX(id) FROM rol));
SELECT setval(pg_get_serial_sequence('usuario', 'id'),
              (SELECT MAX(id) FROM usuario));

-- Configuracion por defecto del dueno (un solo registro)
INSERT INTO configuracion_dueno (
    toggle_email_instantaneo,
    toggle_email_digest,
    toggle_resumen_diario,
    meta_ventas_mensual,
    meta_derivados_por_colaborador,
    plazo_reevaluacion_sbs_dias,
    max_intentos_no_contesto,
    regla_reintento_no_contesto,
    hora_inicio_jornada,
    minutos_gracia_ausencia
) VALUES (FALSE, FALSE, FALSE, 0, 0, 90, 6, '+3h,+24h,+48h,+72h,+120h', '09:00', 45);

-- Feriados nacionales de Peru 2026
-- Fuente: Decreto Supremo N° 003-2019-PCM y actualizaciones
INSERT INTO calendario_laboral (fecha, es_feriado, descripcion) VALUES
    ('2026-01-01', TRUE, 'Año Nuevo'),
    ('2026-04-02', TRUE, 'Jueves Santo'),
    ('2026-04-03', TRUE, 'Viernes Santo'),
    ('2026-05-01', TRUE, 'Día del Trabajo'),
    ('2026-06-07', TRUE, 'Batalla de Arica y Día de la Bandera'),
    ('2026-06-29', TRUE, 'San Pedro y San Pablo'),
    ('2026-07-28', TRUE, 'Fiestas Patrias — Día de la Independencia'),
    ('2026-07-29', TRUE, 'Fiestas Patrias — Gran Parada Militar'),
    ('2026-08-30', TRUE, 'Santa Rosa de Lima'),
    ('2026-10-08', TRUE, 'Combate de Angamos'),
    ('2026-11-01', TRUE, 'Todos los Santos'),
    ('2026-12-08', TRUE, 'Inmaculada Concepción'),
    ('2026-12-09', TRUE, 'Batalla de Ayacucho'),
    ('2026-12-25', TRUE, 'Navidad');
