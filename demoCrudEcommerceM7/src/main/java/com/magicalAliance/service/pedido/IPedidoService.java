package com.magicalAliance.service.pedido;

import com.magicalAliance.dto.carrito.CheckoutDTO;
import com.magicalAliance.entity.carrito.Carrito;
import com.magicalAliance.entity.pedido.EstadoPedido;
import com.magicalAliance.entity.pedido.Pedido;
import com.magicalAliance.entity.usuario.Cliente;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface IPedidoService {

    /**
     * Valida stock, crea el Pedido con snapshots, descuenta stock y marca el carrito como COMPLETADO.
     * Lanza MagicalBusinessException si algún producto no tiene stock suficiente.
     */
    Pedido crearDesdeCarrito(Carrito carrito, CheckoutDTO dto, Cliente cliente);

    Optional<Pedido> buscarPorId(Long id);

    List<Pedido> listarPorCliente(Cliente cliente);

    List<Pedido> listarTodos(String filtroEstado, LocalDate fechaDesde, LocalDate fechaHasta, String emailBuscar, String orden);

    void actualizarEstado(Long pedidoId, EstadoPedido nuevoEstado);

    /**
     * Cancela un pedido si aún no ha sido enviado. Restaura el stock de cada ítem.
     * Solo puede hacerlo el propio cliente o un admin.
     */
    void cancelarPedido(Long pedidoId, Cliente cliente);

    /**
     * Solicita devolución de un pedido que ya fue enviado.
     * Solo puede hacerlo el propio cliente o un admin.
     */
    void solicitarDevolucion(Long pedidoId, Cliente cliente);
}