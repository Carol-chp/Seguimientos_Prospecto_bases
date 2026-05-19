package com.pe.swcotoschero.prospectos.Repository;

import com.pe.swcotoschero.prospectos.Entity.TarjetaWhatsapp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TarjetaWhatsappRepository extends JpaRepository<TarjetaWhatsapp, Long> {
}
