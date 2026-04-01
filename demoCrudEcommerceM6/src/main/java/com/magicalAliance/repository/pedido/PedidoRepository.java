package com.magicalAliance.repository.pedido;

import com.magicalAliance.entity.pedido.EstadoPedido;
import com.magicalAliance.entity.pedido.Pedido;
import com.magicalAliance.entity.usuario.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    List<Pedido> findByClienteOrderByFechaPedidoDesc(Cliente cliente);

    List<Pedido> findAllByOrderByFechaPedidoDesc();

    List<Pedido> findByEstadoOrderByFechaPedidoDesc(EstadoPedido estado);

    // ── Con filtro de fechas ───────────────────────────────────────────────────
    List<Pedido> findAllByFechaPedidoBetweenOrderByFechaPedidoDesc(LocalDateTime desde, LocalDateTime hasta);

    List<Pedido> findByEstadoAndFechaPedidoBetweenOrderByFechaPedidoDesc(EstadoPedido estado, LocalDateTime desde, LocalDateTime hasta);
}