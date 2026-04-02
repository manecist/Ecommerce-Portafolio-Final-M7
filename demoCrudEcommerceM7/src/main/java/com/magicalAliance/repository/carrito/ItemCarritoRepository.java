package com.magicalAliance.repository.carrito;

import com.magicalAliance.entity.carrito.ItemCarrito;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemCarritoRepository extends JpaRepository<ItemCarrito, Long> {
}