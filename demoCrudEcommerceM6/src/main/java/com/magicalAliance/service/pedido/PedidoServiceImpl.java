package com.magicalAliance.service.pedido;

import com.magicalAliance.dto.carrito.CheckoutDTO;
import com.magicalAliance.entity.carrito.Carrito;
import com.magicalAliance.entity.carrito.EstadoCarrito;
import com.magicalAliance.entity.carrito.ItemCarrito;
import com.magicalAliance.entity.pedido.EstadoPedido;
import com.magicalAliance.entity.pedido.ItemPedido;
import com.magicalAliance.entity.pedido.Pedido;
import com.magicalAliance.entity.usuario.Cliente;
import com.magicalAliance.exception.MagicalBusinessException;
import com.magicalAliance.repository.carrito.CarritoRepository;
import com.magicalAliance.repository.pedido.PedidoRepository;
import com.magicalAliance.repository.producto.ProductoRepository;
import com.magicalAliance.service.IEmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class PedidoServiceImpl implements IPedidoService {

    private static final double TASA_IVA = 0.19;

    @Autowired private PedidoRepository pedidoRepo;
    @Autowired private CarritoRepository carritoRepo;
    @Autowired private ProductoRepository productoRepo;
    @Autowired private IEmailService emailService;

    // ─── Crear pedido desde carrito ──────────────────────────────────────────

    @Override
    public Pedido crearDesdeCarrito(Carrito carrito, CheckoutDTO dto, Cliente cliente) {

        if (carrito.getItems().isEmpty()) {
            throw new MagicalBusinessException("El carrito está vacío. Agrega productos antes de continuar");
        }

        // 1. Validar stock de TODOS los ítems antes de procesar ninguno
        List<String> erroresStock = new ArrayList<>();
        for (ItemCarrito item : carrito.getItems()) {
            if (item.getProducto().getStock() < item.getCantidad()) {
                erroresStock.add("'" + item.getProducto().getNombre() + "': "
                        + "solicitado " + item.getCantidad()
                        + ", disponible " + item.getProducto().getStock());
            }
        }
        if (!erroresStock.isEmpty()) {
            throw new MagicalBusinessException(
                    "Stock insuficiente para: " + String.join(", ", erroresStock));
        }

        // 2. Calcular subtotal, IVA y total
        double subtotal = carrito.getTotal();
        double iva      = subtotal * TASA_IVA;
        double total    = subtotal + iva;

        // 3. Construir snapshot de dirección a partir de los campos separados
        String direccionEntrega = dto.buildDireccionEntrega();

        // 4. Construir el Pedido
        Pedido pedido = Pedido.builder()
                .cliente(cliente)
                .nombreContacto(dto.getNombreContacto())
                .emailContacto(dto.getEmailContacto())
                .telefonoContacto(dto.getTelefonoContacto())
                .direccionEntrega(direccionEntrega)
                .notasPedido(dto.getNotasPedido())
                .subtotal(subtotal)
                .iva(iva)
                .total(total)
                .estado(EstadoPedido.PENDIENTE)
                .build();

        // 5. Crear snapshots de ítems
        for (ItemCarrito item : carrito.getItems()) {
            ItemPedido itemPedido = ItemPedido.builder()
                    .pedido(pedido)
                    .producto(item.getProducto())
                    .nombreProducto(item.getProducto().getNombre())
                    .imagenProducto(item.getProducto().getImagen())
                    .cantidad(item.getCantidad())
                    .precioUnitario(item.getPrecioUnitario())
                    .build();
            pedido.getItems().add(itemPedido);
        }

        Pedido pedidoGuardado = pedidoRepo.save(pedido);

        // 6. Descontar stock (ocurre al confirmar, no al agregar al carrito)
        for (ItemCarrito item : carrito.getItems()) {
            item.getProducto().setStock(item.getProducto().getStock() - item.getCantidad());
            productoRepo.save(item.getProducto());
        }

        // 7. Marcar carrito como COMPLETADO
        carrito.setEstado(EstadoCarrito.COMPLETADO);
        carritoRepo.save(carrito);

        // 8. Enviar correo de confirmación al cliente (fallo silencioso)
        emailService.enviarConfirmacionPedido(pedidoGuardado);

        return pedidoGuardado;
    }

    // ─── Consultas ───────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Optional<Pedido> buscarPorId(Long id) {
        return pedidoRepo.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Pedido> listarPorCliente(Cliente cliente) {
        return pedidoRepo.findByClienteOrderByFechaPedidoDesc(cliente);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Pedido> listarTodos(String filtroEstado, LocalDate fechaDesde, LocalDate fechaHasta) {
        boolean filtraTodos = filtroEstado == null || filtroEstado.isBlank() || filtroEstado.equals("TODOS");
        boolean filtraFechas = fechaDesde != null || fechaHasta != null;

        // Normalizar rango: si falta uno de los extremos, usar un rango amplio
        LocalDateTime desde = fechaDesde != null
                ? fechaDesde.atStartOfDay()
                : LocalDateTime.of(2000, 1, 1, 0, 0);
        LocalDateTime hasta = fechaHasta != null
                ? fechaHasta.atTime(23, 59, 59)
                : LocalDateTime.now();

        if (filtraTodos) {
            return filtraFechas
                    ? pedidoRepo.findAllByFechaPedidoBetweenOrderByFechaPedidoDesc(desde, hasta)
                    : pedidoRepo.findAllByOrderByFechaPedidoDesc();
        }

        try {
            EstadoPedido estado = EstadoPedido.valueOf(filtroEstado);
            return filtraFechas
                    ? pedidoRepo.findByEstadoAndFechaPedidoBetweenOrderByFechaPedidoDesc(estado, desde, hasta)
                    : pedidoRepo.findByEstadoOrderByFechaPedidoDesc(estado);
        } catch (IllegalArgumentException e) {
            return filtraFechas
                    ? pedidoRepo.findAllByFechaPedidoBetweenOrderByFechaPedidoDesc(desde, hasta)
                    : pedidoRepo.findAllByOrderByFechaPedidoDesc();
        }
    }

    // ─── Actualizar estado (admin) ───────────────────────────────────────────

    @Override
    public void actualizarEstado(Long pedidoId, EstadoPedido nuevoEstado) {
        Pedido pedido = pedidoRepo.findById(pedidoId)
                .orElseThrow(() -> new MagicalBusinessException("Pedido #" + pedidoId + " no encontrado"));
        pedido.setEstado(nuevoEstado);
        pedidoRepo.save(pedido);

        // Notificar al cliente del cambio de estado
        emailService.enviarActualizacionEstadoPedido(pedido);
    }

    // ─── Cancelar pedido (cliente) ───────────────────────────────────────────

    @Override
    public void cancelarPedido(Long pedidoId, Cliente cliente) {
        Pedido pedido = pedidoRepo.findById(pedidoId)
                .orElseThrow(() -> new MagicalBusinessException("Pedido #" + pedidoId + " no encontrado"));

        // Verificar propiedad
        if (pedido.getCliente() == null || !pedido.getCliente().getId().equals(cliente.getId())) {
            throw new MagicalBusinessException("No tienes permiso para cancelar este pedido");
        }

        // Solo se puede cancelar si no ha sido enviado/entregado ni ya cancelado
        EstadoPedido estado = pedido.getEstado();
        if (estado == EstadoPedido.ENVIADO || estado == EstadoPedido.ENTREGADO
                || estado == EstadoPedido.CANCELADO || estado == EstadoPedido.DEVOLUCION_SOLICITADA) {
            throw new MagicalBusinessException(
                    "No es posible cancelar el pedido en estado: " + estado);
        }

        // Restaurar stock de cada ítem
        for (ItemPedido item : pedido.getItems()) {
            if (item.getProducto() != null) {
                item.getProducto().setStock(item.getProducto().getStock() + item.getCantidad());
                productoRepo.save(item.getProducto());
            }
        }

        pedido.setEstado(EstadoPedido.CANCELADO);
        pedidoRepo.save(pedido);

        emailService.enviarActualizacionEstadoPedido(pedido);
    }

    // ─── Solicitar devolución (cliente) ─────────────────────────────────────

    @Override
    public void solicitarDevolucion(Long pedidoId, Cliente cliente) {
        Pedido pedido = pedidoRepo.findById(pedidoId)
                .orElseThrow(() -> new MagicalBusinessException("Pedido #" + pedidoId + " no encontrado"));

        // Verificar propiedad
        if (pedido.getCliente() == null || !pedido.getCliente().getId().equals(cliente.getId())) {
            throw new MagicalBusinessException("No tienes permiso para solicitar devolución de este pedido");
        }

        // Solo se puede solicitar devolución si el pedido fue enviado
        if (pedido.getEstado() != EstadoPedido.ENVIADO) {
            throw new MagicalBusinessException(
                    "Solo puedes solicitar devolución de pedidos con estado ENVIADO");
        }

        pedido.setEstado(EstadoPedido.DEVOLUCION_SOLICITADA);
        pedidoRepo.save(pedido);

        emailService.enviarActualizacionEstadoPedido(pedido);
    }
}