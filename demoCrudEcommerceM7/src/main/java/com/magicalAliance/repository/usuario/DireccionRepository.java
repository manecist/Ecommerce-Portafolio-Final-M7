package com.magicalAliance.repository.usuario;

import com.magicalAliance.entity.usuario.DireccionCliente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DireccionRepository extends JpaRepository<DireccionCliente, Long> {
    Optional<DireccionCliente> findById(Long direccionId);
}
