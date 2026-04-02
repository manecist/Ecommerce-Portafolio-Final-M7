package com.magicalAliance.repository.usuario;

import com.magicalAliance.entity.usuario.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmail(String email);

    Optional<Usuario> findByClienteRut(String rut);

    @Query("SELECT u FROM Usuario u WHERE " +
            "LOWER(u.cliente.rut) LIKE LOWER(CONCAT('%', :t, '%')) OR " +
            "LOWER(u.email) LIKE LOWER(CONCAT('%', :t, '%')) OR " +
            "LOWER(u.cliente.nombre) LIKE LOWER(CONCAT('%', :t, '%')) OR " +
            "LOWER(u.cliente.apellido) LIKE LOWER(CONCAT('%', :t, '%'))")
    List<Usuario> buscarPorCriterioFlexible(@Param("t") String t);
}
