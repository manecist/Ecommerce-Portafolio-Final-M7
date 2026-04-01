package com.magicalAliance.service.carrito;

import com.magicalAliance.entity.carrito.Carrito;
import com.magicalAliance.entity.carrito.EstadoCarrito;
import com.magicalAliance.entity.carrito.ItemCarrito;
import com.magicalAliance.entity.producto.Producto;
import com.magicalAliance.entity.usuario.Cliente;
import com.magicalAliance.entity.usuario.Usuario;
import com.magicalAliance.exception.MagicalBusinessException;
import com.magicalAliance.repository.carrito.CarritoRepository;
import com.magicalAliance.repository.carrito.ItemCarritoRepository;
import com.magicalAliance.repository.producto.ProductoRepository;
import com.magicalAliance.repository.usuario.UsuarioRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
public class CarritoServiceImpl implements ICarritoService {

    @Autowired private CarritoRepository carritoRepo;
    @Autowired private ItemCarritoRepository itemRepo;
    @Autowired private ProductoRepository productoRepo;
    @Autowired private UsuarioRepository usuarioRepo;

    // ─── Obtener o crear carrito ─────────────────────────────────────────────

    @Override
    public Carrito obtenerOCrearCarrito(Authentication auth, HttpSession session) {

        if (estaAutenticado(auth)) {
            Cliente cliente = obtenerClienteDeAuth(auth);

            // Si el usuario recién inició sesión y tiene un carrito de invitado en sesión → fusionar
            Long carritoInvitadoId = (Long) session.getAttribute("carritoId");
            if (carritoInvitadoId != null) {
                fusionarConCliente(carritoInvitadoId, cliente);
                session.removeAttribute("carritoId");
            }

            // Devolver o crear el carrito del cliente
            return carritoRepo.findByClienteAndEstado(cliente, EstadoCarrito.ACTIVO)
                    .orElseGet(() -> crearNuevoCarrito(cliente, session));
        }

        // Invitado: buscar por ID guardado en sesión
        Long carritoId = (Long) session.getAttribute("carritoId");
        if (carritoId != null) {
            Optional<Carrito> encontrado = carritoRepo.findById(carritoId);
            if (encontrado.isPresent() && encontrado.get().getEstado() == EstadoCarrito.ACTIVO) {
                return encontrado.get();
            }
        }

        // No hay carrito en sesión → crear uno nuevo y guardar su ID
        return crearNuevoCarrito(null, session);
    }

    private Carrito crearNuevoCarrito(Cliente cliente, HttpSession session) {
        Carrito carrito = Carrito.builder()
                .cliente(cliente)
                .estado(EstadoCarrito.ACTIVO)
                .build();
        carrito = carritoRepo.save(carrito);

        // Si es invitado, persistir el ID en sesión para recuperarlo luego
        if (cliente == null && session != null) {
            session.setAttribute("carritoId", carrito.getId());
        }
        return carrito;
    }

    // ─── Agregar producto ────────────────────────────────────────────────────

    @Override
    public void agregarProducto(Carrito carrito, Long productoId, int cantidad) {
        Producto producto = productoRepo.findById(productoId)
                .orElseThrow(() -> new MagicalBusinessException("El producto no existe en el catálogo"));

        if (producto.getStock() <= 0) {
            throw new MagicalBusinessException("'" + producto.getNombre() + "' está agotado");
        }

        // Si ya existe en el carrito → sumar cantidad
        Optional<ItemCarrito> itemExistente = carrito.getItems().stream()
                .filter(i -> i.getProducto().getId().equals(productoId))
                .findFirst();

        if (itemExistente.isPresent()) {
            ItemCarrito item = itemExistente.get();
            int nuevaCantidad = item.getCantidad() + cantidad;
            if (nuevaCantidad > producto.getStock()) {
                throw new MagicalBusinessException(
                        "Solo hay " + producto.getStock() + " unidades disponibles de '" + producto.getNombre() + "'");
            }
            item.setCantidad(nuevaCantidad);
            itemRepo.save(item);
        } else {
            if (cantidad > producto.getStock()) {
                throw new MagicalBusinessException(
                        "Solo hay " + producto.getStock() + " unidades disponibles de '" + producto.getNombre() + "'");
            }
            ItemCarrito nuevoItem = ItemCarrito.builder()
                    .carrito(carrito)
                    .producto(producto)
                    .cantidad(cantidad)
                    .precioUnitario(producto.getPrecio())
                    .build();
            carrito.getItems().add(nuevoItem);
            itemRepo.save(nuevoItem);
        }

        carritoRepo.save(carrito);
    }

    // ─── Actualizar cantidad ─────────────────────────────────────────────────

    @Override
    public void actualizarCantidad(Long itemId, int nuevaCantidad) {
        ItemCarrito item = itemRepo.findById(itemId)
                .orElseThrow(() -> new MagicalBusinessException("Ítem no encontrado en el carrito"));

        if (nuevaCantidad <= 0) {
            eliminarItem(itemId);
            return;
        }

        Producto producto = item.getProducto();
        if (nuevaCantidad > producto.getStock()) {
            throw new MagicalBusinessException(
                    "Solo hay " + producto.getStock() + " unidades disponibles de '" + producto.getNombre() + "'");
        }

        item.setCantidad(nuevaCantidad);
        itemRepo.save(item);
    }

    // ─── Eliminar ítem ───────────────────────────────────────────────────────

    @Override
    public void eliminarItem(Long itemId) {
        ItemCarrito item = itemRepo.findById(itemId)
                .orElseThrow(() -> new MagicalBusinessException("Ítem no encontrado en el carrito"));
        Carrito carrito = item.getCarrito();
        carrito.getItems().remove(item);
        itemRepo.delete(item);
        carritoRepo.save(carrito);
    }

    // ─── Vaciar carrito ──────────────────────────────────────────────────────

    @Override
    public void vaciarCarrito(Carrito carrito) {
        carrito.getItems().clear();
        carritoRepo.save(carrito);
    }

    // ─── Fusionar carrito invitado con cliente ───────────────────────────────

    @Override
    public void fusionarConCliente(Long carritoInvitadoId, Cliente cliente) {
        Optional<Carrito> carritoInvitadoOpt = carritoRepo.findById(carritoInvitadoId);
        if (carritoInvitadoOpt.isEmpty() ||
                carritoInvitadoOpt.get().getEstado() != EstadoCarrito.ACTIVO) {
            return;
        }

        Carrito carritoInvitado = carritoInvitadoOpt.get();

        Optional<Carrito> carritoPrincipalOpt =
                carritoRepo.findByClienteAndEstado(cliente, EstadoCarrito.ACTIVO);

        if (carritoPrincipalOpt.isEmpty()) {
            // El cliente no tiene carrito activo → simplemente asignarle el carrito del invitado
            carritoInvitado.setCliente(cliente);
            carritoRepo.save(carritoInvitado);
        } else {
            // Fusionar ítems del carrito de invitado al carrito existente del cliente
            Carrito carritoPrincipal = carritoPrincipalOpt.get();

            for (ItemCarrito itemInv : carritoInvitado.getItems()) {
                Long prodId = itemInv.getProducto().getId();

                Optional<ItemCarrito> itemExistente = carritoPrincipal.getItems().stream()
                        .filter(i -> i.getProducto().getId().equals(prodId))
                        .findFirst();

                if (itemExistente.isPresent()) {
                    // Sumar cantidades (sin superar stock — en checkout se valida)
                    itemExistente.get().setCantidad(
                            itemExistente.get().getCantidad() + itemInv.getCantidad());
                } else {
                    ItemCarrito nuevoItem = ItemCarrito.builder()
                            .carrito(carritoPrincipal)
                            .producto(itemInv.getProducto())
                            .cantidad(itemInv.getCantidad())
                            .precioUnitario(itemInv.getPrecioUnitario())
                            .build();
                    carritoPrincipal.getItems().add(nuevoItem);
                }
            }

            carritoRepo.save(carritoPrincipal);

            // Marcar carrito del invitado como abandonado
            carritoInvitado.setEstado(EstadoCarrito.ABANDONADO);
            carritoRepo.save(carritoInvitado);
        }
    }

    // ─── Contar ítems para el badge del navbar ───────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public int contarItems(Authentication auth, HttpSession session) {
        try {
            if (estaAutenticado(auth)) {
                Cliente cliente = obtenerClienteDeAuth(auth);
                return carritoRepo.findByClienteAndEstado(cliente, EstadoCarrito.ACTIVO)
                        .map(c -> c.getItems().stream().mapToInt(ItemCarrito::getCantidad).sum())
                        .orElse(0);
            }

            Long carritoId = (Long) session.getAttribute("carritoId");
            if (carritoId == null) return 0;

            return carritoRepo.findById(carritoId)
                    .filter(c -> c.getEstado() == EstadoCarrito.ACTIVO)
                    .map(c -> c.getItems().stream().mapToInt(ItemCarrito::getCantidad).sum())
                    .orElse(0);

        } catch (Exception e) {
            return 0;
        }
    }

    // ─── Helpers privados ────────────────────────────────────────────────────

    private boolean estaAutenticado(Authentication auth) {
        return auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken);
    }

    private Cliente obtenerClienteDeAuth(Authentication auth) {
        return usuarioRepo.findByEmail(auth.getName())
                .map(Usuario::getCliente)
                .orElseThrow(() -> new MagicalBusinessException("No se encontró el cliente autenticado"));
    }
}