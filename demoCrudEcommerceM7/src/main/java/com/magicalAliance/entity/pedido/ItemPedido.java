package com.magicalAliance.entity.pedido;

import com.magicalAliance.entity.producto.Producto;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "items_pedido")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemPedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;

    // Nullable: el producto puede ser eliminado después, pero el historial del pedido queda intacto
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id")
    private Producto producto;

    // Snapshots — datos del producto al momento de comprar
    @Column(name = "nombre_producto", nullable = false)
    private String nombreProducto;

    @Column(name = "imagen_producto")
    private String imagenProducto;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(name = "precio_unitario", nullable = false)
    private Double precioUnitario;

    // Precio original antes de descuento (snapshot — null si no hubo descuento)
    @Column(name = "precio_original")
    private Double precioOriginal;

    public Double getSubtotal() {
        return precioUnitario * cantidad;
    }

    public boolean tieneDescuento() {
        return precioOriginal != null && precioOriginal > precioUnitario;
    }
}