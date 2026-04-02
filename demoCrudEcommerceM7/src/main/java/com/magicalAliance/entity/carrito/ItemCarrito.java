package com.magicalAliance.entity.carrito;

import com.magicalAliance.entity.producto.Producto;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "items_carrito")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemCarrito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "carrito_id", nullable = false)
    private Carrito carrito;

    // Carga eager para mostrar datos del producto en la vista del carrito
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Column(nullable = false)
    private Integer cantidad;

    // Snapshot del precio al momento de agregar — no cambia si el producto cambia de precio
    @Column(name = "precio_unitario", nullable = false)
    private Double precioUnitario;

    // Precio original antes de aplicar descuento automático (null si no hubo descuento)
    @Column(name = "precio_original")
    private Double precioOriginal;

    public Double getSubtotal() {
        return precioUnitario * cantidad;
    }

    /** Ahorro total generado por descuento en este ítem (0 si no hay descuento). */
    public Double getAhorroPorItem() {
        if (precioOriginal == null || precioOriginal <= precioUnitario) return 0.0;
        return (precioOriginal - precioUnitario) * cantidad;
    }

    public boolean tieneDescuento() {
        return precioOriginal != null && precioOriginal > precioUnitario;
    }
}