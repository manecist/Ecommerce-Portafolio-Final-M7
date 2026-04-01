package com.magicalAliance.repository.carrito;

import com.magicalAliance.entity.carrito.Carrito;
import com.magicalAliance.entity.carrito.EstadoCarrito;
import com.magicalAliance.entity.usuario.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CarritoRepository extends JpaRepository<Carrito, Long> {

    Optional<Carrito> findByClienteAndEstado(Cliente cliente, EstadoCarrito estado);
}