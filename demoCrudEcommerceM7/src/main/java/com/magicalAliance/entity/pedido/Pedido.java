package com.magicalAliance.entity.pedido;

import com.magicalAliance.entity.usuario.Cliente;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pedidos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // null si el pedido fue hecho por un invitado sin cuenta
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    // Datos del invitado (usados cuando cliente es null)
    @Column(name = "nombre_contacto")
    private String nombreContacto;

    @Column(name = "email_contacto")
    private String emailContacto;

    @Column(name = "telefono_contacto")
    private String telefonoContacto;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    @Builder.Default
    private EstadoPedido estado = EstadoPedido.PENDIENTE;

    // Snapshot de la dirección en texto — no cambia si la dirección del cliente cambia
    @Column(name = "direccion_entrega", columnDefinition = "TEXT")
    private String direccionEntrega;

    @Builder.Default
    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    private List<ItemPedido> items = new ArrayList<>();

    // ── Desglose de precios ───────────────────────────────────────────────────
    private Double subtotal;              // neto final (tras todos los descuentos, antes de IVA)
    private Double iva;                   // 19% del subtotal
    private Double total;                 // subtotal + IVA

    // Desglose de descuentos (null cuando no aplica)
    @Column(name = "monto_ahorro_productos")
    private Double montoAhorroProductos;  // ahorro por descuentos automáticos en ítems

    @Column(name = "monto_descuento_cupon")
    private Double montoDescuentoCupon;   // descuento aplicado por cupón

    @Column(name = "cupon_aplicado", length = 50)
    private String cuponAplicado;         // código del cupón usado

    @Column(name = "notas_pedido", columnDefinition = "TEXT")
    private String notasPedido;

    @Column(name = "fecha_pedido")
    private LocalDateTime fechaPedido;

    @PrePersist
    public void onCreate() {
        fechaPedido = LocalDateTime.now();
    }

    // Helper: nombre para mostrar (cliente registrado o invitado)
    public String getNombreParaMostrar() {
        if (cliente != null) {
            return cliente.getNombre() + " " + cliente.getApellido();
        }
        return nombreContacto != null ? nombreContacto : "Invitado";
    }
}