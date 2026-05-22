package com.pe.swcotoschero.prospectos.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "banco")
@Getter
@Setter
public class Banco {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre", nullable = false, unique = true, length = 80)
    private String nombre;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    /**
     * Banco al que se asignan automáticamente las bases nuevas importadas.
     * Solo puede haber uno con esDefault=true a la vez; la lógica de unicidad
     * se garantiza en BancoService al hacer PUT con esDefault=true.
     */
    @Column(name = "es_default", nullable = false)
    private Boolean esDefault = false;

    /**
     * Banco destino al que se reenvían los prospectos OBSERVADO de este banco.
     * Relación self-referencial nullable.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "banco_destino_id")
    private Banco bancoDestino;
}
