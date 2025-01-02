package org.example.Repository;

import org.example.Entity.Prospecto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProspectoRepository extends JpaRepository<Prospecto, Long> {
    List<Prospecto> findByCampaniaAndDistrito(String campania, String distrito);
}

