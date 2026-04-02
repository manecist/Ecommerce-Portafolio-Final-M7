package com.magicalAliance.entity.carrito;

import com.magicalAliance.entity.usuario.Cliente;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "carritos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Carrito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // null si es carrito de invitado sin cuenta
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private EstadoCarrito estado = EstadoCarrito.ACTIVO;

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    // Cupón aplicado al carrito (código en mayúsculas)
    @Column(name = "cupon_aplicado", length = 50)
    private String cuponAplicado;

    // Monto de descuento generado por el cupón (se recalcula al agregar/quitar ítems)
    @Column(name = "monto_descuento_cupon")
    private Double montoDescuentoCupon;

    @Builder.Default
    @OneToMany(mappedBy = "carrito", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<ItemCarrito> items = new ArrayList<>();

    @PrePersist
    public void onCreate() {
        fechaCreacion = LocalDateTime.now();
        fechaActualizacion = LocalDateTime.now();
    }

    @PreUpdate
    public void onUpdate() {
        fechaActualizacion = LocalDateTime.now();
    }

    /** Suma de subtotales de ítems con precios ya descontados (antes de cupón). */
    public double getTotal() {
        return items.stream()
                .mapToDouble(i -> i.getPrecioUnitario() * i.getCantidad())
                .sum();
    }

    /** Total final después de descontar el cupón. */
    public double getTotalConCupon() {
        double descuento = montoDescuentoCupon != null ? montoDescuentoCupon : 0.0;
        return Math.max(0, getTotal() - descuento);
    }

    /** Ahorro total generado por descuentos automáticos en los ítems. */
    public double getAhorroProductos() {
        return items.stream().mapToDouble(ItemCarrito::getAhorroPorItem).sum();
    }

    public int getTotalItems() {
        return items.stream()
                .mapToInt(ItemCarrito::getCantidad)
                .sum();
    }
}