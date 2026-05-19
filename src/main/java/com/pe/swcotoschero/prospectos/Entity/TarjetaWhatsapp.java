package com.pe.swcotoschero.prospectos.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Tarjeta/firma de WhatsApp por colaborador (imagen). Tabla aparte de
 * {@code usuario} para no cargar el blob en cada lectura de Usuario
 * (que ocurre en cada request autenticado).
 */
@Entity
@Table(name = "usuario_tarjeta_whatsapp")
@Getter
@Setter
public class TarjetaWhatsapp {

    /** PK = id del usuario dueño de la tarjeta (1:1). */
    @Id
    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(name = "imagen", nullable = false)
    private byte[] imagen;

    @Column(name = "tipo", nullable = false, length = 64)
    private String tipo;

    @Column(name = "actualizado", nullable = false)
    private LocalDateTime actualizado = LocalDateTime.now();
}
