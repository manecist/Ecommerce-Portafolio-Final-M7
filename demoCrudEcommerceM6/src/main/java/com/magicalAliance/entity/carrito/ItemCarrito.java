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

    public Double getSubtotal() {
        return precioUnitario * cantidad;
    }
}