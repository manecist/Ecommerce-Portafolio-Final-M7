package com.magicalAliance.repository.pedido;

import com.magicalAliance.entity.pedido.EstadoPedido;
import com.magicalAliance.entity.pedido.Pedido;
import com.magicalAliance.entity.usuario.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    List<Pedido> findByClienteOrderByFechaPedidoDesc(Cliente cliente);

    List<Pedido> findAllByOrderByFechaPedidoDesc();

    List<Pedido> findByEstadoOrderByFechaPedidoDesc(EstadoPedido estado);

    // ── Con filtro de fechas ───────────────────────────────────────────────────
    List<Pedido> findAllByFechaPedidoBetweenOrderByFechaPedidoDesc(LocalDateTime desde, LocalDateTime hasta);

    List<Pedido> findByEstadoAndFechaPedidoBetweenOrderByFechaPedidoDesc(EstadoPedido estado, LocalDateTime desde, LocalDateTime hasta);

    // ── Con filtro de email (búsqueda parcial) ─────────────────────────────────
    @Query("SELECT p FROM Pedido p WHERE LOWER(p.emailContacto) LIKE LOWER(CONCAT('%', :email, '%')) ORDER BY p.fechaPedido DESC")
    List<Pedido> findByEmailContainingIgnoreCase(@Param("email") String email);

    @Query("SELECT p FROM Pedido p WHERE p.estado = :estado AND LOWER(p.emailContacto) LIKE LOWER(CONCAT('%', :email, '%')) ORDER BY p.fechaPedido DESC")
    List<Pedido> findByEstadoAndEmailContainingIgnoreCase(@Param("estado") EstadoPedido estado, @Param("email") String email);

    @Query("SELECT p FROM Pedido p WHERE p.fechaPedido BETWEEN :desde AND :hasta AND LOWER(p.emailContacto) LIKE LOWER(CONCAT('%', :email, '%')) ORDER BY p.fechaPedido DESC")
    List<Pedido> findByEmailAndFechaBetween(@Param("email") String email, @Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    @Query("SELECT p FROM Pedido p WHERE p.estado = :estado AND p.fechaPedido BETWEEN :desde AND :hasta AND LOWER(p.emailContacto) LIKE LOWER(CONCAT('%', :email, '%')) ORDER BY p.fechaPedido DESC")
    List<Pedido> findByEstadoAndEmailAndFechaBetween(@Param("estado") EstadoPedido estado, @Param("email") String email, @Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    // ── Vinculación de pedidos de invitado al registrarse ────────────────────
    @org.springframework.data.jpa.repository.Modifying
    @Query("UPDATE Pedido p SET p.cliente = :cliente WHERE p.cliente IS NULL AND LOWER(p.emailContacto) = LOWER(:email)")
    int vincularPedidosInvitado(@Param("cliente") Cliente cliente, @Param("email") String email);
}