package com.pe.swcotoschero.prospectos.Repository;

import com.pe.swcotoschero.prospectos.Entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByUsuarioAndEstado(String usuario, Boolean estado);
    
    /**
     * Buscar usuarios activos que no sean administradores
     */
    @Query("SELECT u FROM Usuario u WHERE u.estado = true AND u.rol.id != :adminRoleId ORDER BY u.nombre ASC")
    List<Usuario> findActiveUsersWithoutAdminRole(@Param("adminRoleId") Long adminRoleId);
    
    /**
     * Buscar usuarios por rol
     */
    List<Usuario> findByRol_IdAndEstadoOrderByNombreAsc(Long rolId, Boolean estado);
}
