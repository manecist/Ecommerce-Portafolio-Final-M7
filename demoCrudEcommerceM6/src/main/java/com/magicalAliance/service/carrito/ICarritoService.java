package com.magicalAliance.service.carrito;

import com.magicalAliance.entity.carrito.Carrito;
import com.magicalAliance.entity.usuario.Cliente;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;

public interface ICarritoService {

    /**
     * Devuelve el carrito activo del usuario autenticado o del invitado (por sesión).
     * Si no existe, lo crea. Si el usuario acaba de iniciar sesión y hay un carrito
     * de invitado en la sesión, fusiona automáticamente antes de devolver.
     */
    Carrito obtenerOCrearCarrito(Authentication auth, HttpSession session);

    /**
     * Agrega un producto al carrito. Si ya existe, suma la cantidad.
     * No rebaja stock — eso ocurre solo al confirmar el pedido.
     */
    void agregarProducto(Carrito carrito, Long productoId, int cantidad);

    /**
     * Actualiza la cantidad de un ítem. Si cantidad <= 0, elimina el ítem.
     */
    void actualizarCantidad(Long itemId, int nuevaCantidad);

    /**
     * Elimina un ítem del carrito por su ID.
     */
    void eliminarItem(Long itemId);

    /**
     * Vacía todos los ítems del carrito (sin eliminarlo).
     */
    void vaciarCarrito(Carrito carrito);

    /**
     * Fusiona un carrito de invitado con el carrito del cliente registrado.
     * Si el cliente ya tiene carrito activo, suma ítems. Si no, asigna el carrito.
     */
    void fusionarConCliente(Long carritoInvitadoId, Cliente cliente);

    /**
     * Retorna la cantidad total de ítems (suma de cantidades) para el badge del nav.
     */
    int contarItems(Authentication auth, HttpSession session);
}