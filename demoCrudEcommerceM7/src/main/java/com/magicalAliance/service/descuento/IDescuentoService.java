package com.magicalAliance.service.descuento;

import com.magicalAliance.entity.carrito.Carrito;
import com.magicalAliance.entity.descuento.Cupon;
import com.magicalAliance.entity.descuento.Descuento;
import com.magicalAliance.entity.producto.Producto;

import java.util.List;
import java.util.Optional;

public interface IDescuentoService {

    // ── Descuentos automáticos ──────────────────────────────────────────────

    /**
     * Busca el mejor descuento activo para un producto revisando todos los niveles:
     * producto específico > subcategoría > categoría > global.
     * Retorna el descuento que genera mayor ahorro para el cliente.
     */
    Optional<Descuento> obtenerMejorDescuento(Producto producto);

    /**
     * Devuelve el precio efectivo del producto después de aplicar el mejor descuento activo.
     * Si no hay descuento, retorna el precio original del producto.
     */
    double calcularPrecioConDescuento(Producto producto);

    // ── Cupones ─────────────────────────────────────────────────────────────

    /**
     * Valida un código de cupón para el total de carrito dado.
     * Lanza MagicalBusinessException con mensaje descriptivo si no es válido.
     */
    Cupon validarCupon(String codigo, double totalCarrito);

    /**
     * Aplica un cupón al carrito: valida, calcula el monto y persiste los cambios.
     * Lanza MagicalBusinessException si el cupón no es válido.
     */
    void aplicarCupon(Carrito carrito, String codigo);

    /** Quita el cupón aplicado al carrito y persiste los cambios. */
    void quitarCupon(Carrito carrito);

    // ── Admin CRUD Descuentos ───────────────────────────────────────────────

    List<Descuento> listarDescuentos();
    Descuento guardarDescuento(Descuento descuento);
    Optional<Descuento> buscarDescuentoPorId(Long id);
    void eliminarDescuento(Long id);

    // ── Admin CRUD Cupones ──────────────────────────────────────────────────

    List<Cupon> listarCupones();
    Cupon guardarCupon(Cupon cupon);
    Optional<Cupon> buscarCuponPorId(Long id);
    void eliminarCupon(Long id);
}
