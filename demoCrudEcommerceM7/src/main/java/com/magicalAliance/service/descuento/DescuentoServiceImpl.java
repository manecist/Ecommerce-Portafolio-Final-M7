package com.magicalAliance.service.descuento;

import com.magicalAliance.entity.carrito.Carrito;
import com.magicalAliance.entity.descuento.Cupon;
import com.magicalAliance.entity.descuento.Descuento;
import com.magicalAliance.entity.producto.Producto;
import com.magicalAliance.exception.MagicalBusinessException;
import com.magicalAliance.repository.carrito.CarritoRepository;
import com.magicalAliance.repository.descuento.CuponRepository;
import com.magicalAliance.repository.descuento.DescuentoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class DescuentoServiceImpl implements IDescuentoService {

    @Autowired private DescuentoRepository descuentoRepo;
    @Autowired private CuponRepository cuponRepo;
    @Autowired private CarritoRepository carritoRepo;

    // ── Descuentos automáticos ──────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Optional<Descuento> obtenerMejorDescuento(Producto producto) {
        LocalDate hoy = LocalDate.now();
        List<Descuento> candidatos = new ArrayList<>();

        candidatos.addAll(descuentoRepo.findActivosParaProducto(producto.getId(), hoy));
        candidatos.addAll(descuentoRepo.findActivosParaSubcategoria(producto.getSubcategoria().getId(), hoy));
        candidatos.addAll(descuentoRepo.findActivosParaCategoria(
                producto.getSubcategoria().getCategoria().getId(), hoy));
        candidatos.addAll(descuentoRepo.findActivosGlobales(hoy));

        // Gana el descuento que genera mayor ahorro para el cliente
        return candidatos.stream()
                .max(Comparator.comparingDouble(d -> d.calcularAhorro(producto.getPrecio())));
    }

    @Override
    @Transactional(readOnly = true)
    public double calcularPrecioConDescuento(Producto producto) {
        return obtenerMejorDescuento(producto)
                .map(d -> d.calcularPrecioFinal(producto.getPrecio()))
                .orElse(producto.getPrecio());
    }

    // ── Cupones ─────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Cupon validarCupon(String codigo, double totalCarrito) {
        Cupon cupon = cuponRepo.findByCodigoIgnoreCase(codigo)
                .orElseThrow(() -> new MagicalBusinessException(
                        "El código \"" + codigo + "\" no existe"));

        if (!cupon.isActivo()) {
            throw new MagicalBusinessException("El cupón \"" + codigo + "\" está desactivado");
        }
        if (cupon.getFechaExpiracion() != null && LocalDate.now().isAfter(cupon.getFechaExpiracion())) {
            throw new MagicalBusinessException("El cupón \"" + codigo + "\" ha expirado");
        }
        if (cupon.getLimiteUsos() != null && cupon.getUsosActuales() >= cupon.getLimiteUsos()) {
            throw new MagicalBusinessException("El cupón \"" + codigo + "\" ha alcanzado su límite de usos");
        }
        if (totalCarrito < cupon.getMontoMinimo()) {
            throw new MagicalBusinessException(
                    "El cupón requiere un mínimo de $" +
                    String.format("%,.0f", cupon.getMontoMinimo()) + " en tu carrito");
        }
        return cupon;
    }

    @Override
    public void aplicarCupon(Carrito carrito, String codigo) {
        double total = carrito.getTotal();
        Cupon cupon = validarCupon(codigo, total);
        carrito.setCuponAplicado(cupon.getCodigo().toUpperCase());
        carrito.setMontoDescuentoCupon(cupon.calcularDescuento(total));
        carritoRepo.save(carrito);
    }

    @Override
    public void quitarCupon(Carrito carrito) {
        carrito.setCuponAplicado(null);
        carrito.setMontoDescuentoCupon(null);
        carritoRepo.save(carrito);
    }

    // ── Admin CRUD Descuentos ───────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<Descuento> listarDescuentos() {
        return descuentoRepo.findAllByOrderByFechaInicioDesc();
    }

    @Override
    public Descuento guardarDescuento(Descuento descuento) {
        // Limpiar referencias que no corresponden al alcance seleccionado
        switch (descuento.getAlcance()) {
            case GLOBAL -> {
                descuento.setCategoria(null);
                descuento.setSubcategoria(null);
                descuento.setProducto(null);
            }
            case CATEGORIA -> {
                descuento.setSubcategoria(null);
                descuento.setProducto(null);
            }
            case SUBCATEGORIA -> {
                descuento.setCategoria(null);
                descuento.setProducto(null);
            }
            case PRODUCTO -> {
                descuento.setCategoria(null);
                descuento.setSubcategoria(null);
            }
        }
        return descuentoRepo.save(descuento);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Descuento> buscarDescuentoPorId(Long id) {
        return descuentoRepo.findById(id);
    }

    @Override
    public void eliminarDescuento(Long id) {
        descuentoRepo.deleteById(id);
    }

    // ── Admin CRUD Cupones ──────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<Cupon> listarCupones() {
        return cuponRepo.findAllByOrderByIdDesc();
    }

    @Override
    public Cupon guardarCupon(Cupon cupon) {
        cupon.setCodigo(cupon.getCodigo().trim().toUpperCase());
        if (cupon.getMontoMinimo() == null) cupon.setMontoMinimo(0.0);
        // Al crear nuevo cupón, los usos comienzan en 0
        if (cupon.getId() == null) cupon.setUsosActuales(0);
        return cuponRepo.save(cupon);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Cupon> buscarCuponPorId(Long id) {
        return cuponRepo.findById(id);
    }

    @Override
    public void eliminarCupon(Long id) {
        cuponRepo.deleteById(id);
    }
}
