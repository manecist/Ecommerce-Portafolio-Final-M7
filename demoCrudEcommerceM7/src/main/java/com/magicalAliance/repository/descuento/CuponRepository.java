package com.magicalAliance.repository.descuento;

import com.magicalAliance.entity.descuento.Cupon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CuponRepository extends JpaRepository<Cupon, Long> {
    Optional<Cupon> findByCodigoIgnoreCase(String codigo);
    List<Cupon> findAllByOrderByIdDesc();
}
