
-- drop database if exists prospectos;
-- create database prospectos;


create table public.rol(
                           id serial,
                           nombre character varying(100) NOT NULL,
                           estado boolean DEFAULT true,
                           CONSTRAINT rol_pkey PRIMARY KEY (id)
);

CREATE TABLE public.usuario
(
    id        serial,
    usuario   character varying(50)  NOT NULL,
    password  character varying(255) NOT NULL,
    nombre    character varying(100) NOT NULL,
    apellidos character varying(100) NOT NULL,
    correo    character varying(100) NOT NULL,
    idrol     integer                NOT NULL,
    estado    boolean                DEFAULT true,
    CONSTRAINT usuario_pkey PRIMARY KEY (id),
    CONSTRAINT usuario_rol_fkey FOREIGN KEY (idrol) REFERENCES public.rol (id)
);



CREATE TABLE public.campania
(
    id          serial,
    nombre      character varying(100) NOT NULL,
    descripcion character varying(255),
    CONSTRAINT campania_pkey PRIMARY KEY (id)
);

CREATE TABLE public.prospecto
(
    id                 serial,
    nombre             character varying(100) NOT NULL,
    apellido           character varying(100) NOT NULL,
    celular            character varying(15)  NOT NULL,
    documentoidentidad character varying(20)  NOT NULL,
    sexo               character varying(10),
    banco              character varying(50),
    cargo              character varying(50),
    distrito           character varying(50),
    campania_id        integer,
    subcampania        character varying(255),
    CONSTRAINT prospecto_pkey PRIMARY KEY (id),
    CONSTRAINT prospecto_campania_fkey FOREIGN KEY (campania_id) REFERENCES public.campania (id)
);

CREATE TABLE public.asignacion
(
    id                 SERIAL,
    prospecto_id       integer NOT NULL,
    personal_id        integer NOT NULL,
    administrador_id   integer NOT NULL,
    fecha_asignacion   timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    fecha_finalizacion timestamp without time zone,
    estado             character varying(50)       DEFAULT 'Asignado'::character varying,
    CONSTRAINT asignacion_pkey PRIMARY KEY (id),
    CONSTRAINT asignacion_prospecto_id_fkey FOREIGN KEY (prospecto_id) REFERENCES public.prospecto (id),
    CONSTRAINT asignacion_personal_id_fkey FOREIGN KEY (personal_id) REFERENCES public.usuario (id),
    CONSTRAINT asignacion_administrador_id_fkey FOREIGN KEY (administrador_id) REFERENCES public.usuario (id)
);



CREATE TABLE public.contacto
(
    id              integer NOT NULL,
    asignacion_id   integer NOT NULL,
    fecha_contacto  timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    comentario      character varying(50),
    estado_contacto character varying(50)       DEFAULT 'No contactado'::character varying,
    CONSTRAINT contacto_pkey PRIMARY KEY (id),
    CONSTRAINT contacto_asignacion_fkey FOREIGN KEY (asignacion_id) REFERENCES public.asignacion (id)
);

insert into rol (nombre) values ('ADMINISTRADOR');
insert into rol (nombre) values ('TELEOPERADOR');

INSERT INTO public.usuario (id, usuario, password, nombre, apellidos, correo, idrol, estado) VALUES (1, 'JCOTOS', '$2a$10$pfl1ty2Bi4hGqU0edGd73.14tVkYb/s3jEGqXKSiWdBEPor3gIN9C', 'Johnny', 'Cotos', 'jrcotos@gmail.com', 1, true);


insert into campania (nombre, descripcion) values ('Minedu', 'campania1');
insert into campania (nombre, descripcion) values ('ESSALUD Piura', 'ESSALUD Piura');
insert into campania (nombre, descripcion) values ('Poder Judicial', 'Poder Judicial');
insert into prospecto (nombre, apellido, celular, documentoidentidad, sexo, banco, cargo, distrito, campania_id, subcampania) values ('Juan', 'Perez', '123456789', '1111111111', 'M', 'Banco', 'Cargo', 'Distrito', 1, 'Subcampania');
insert into prospecto (nombre, apellido, celular, documentoidentidad, sexo, banco, cargo, distrito, campania_id, subcampania) values ('Pedro', 'Perez', '123456789', '1111111111', 'M', 'Banco', 'Cargo', 'Distrito', 2, 'Subcampania');
insert into prospecto (nombre, apellido, celular, documentoidentidad, sexo, banco, cargo, distrito, campania_id, subcampania) values ('Pedro', 'Perez', '961092025', '1111111111', 'M', 'Banco', 'Cargo', 'Distrito', 2, 'Subcampania');
insert into prospecto (nombre, apellido, celular, documentoidentidad, sexo, banco, cargo, distrito, campania_id, subcampania) values ('Pedro', 'Perez', '961092025', '48201874', 'M', 'Banco', 'Cargo', 'Distrito', 2, 'Subcampania');
insert into prospecto (nombre, apellido, celular, documentoidentidad, sexo, banco, cargo, distrito, campania_id, subcampania) values ('Carlos', 'Perez', '961092026', '48201875', 'M', 'Banco', 'Cargo', 'Distrito', 3, 'Subcampania');

