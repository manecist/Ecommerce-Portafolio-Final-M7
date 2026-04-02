package com.magicalAliance.entity.descuento;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Entity
@Table(name = "cupones")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El código del cupón es obligatorio")
    @Column(nullable = false, unique = true, length = 50)
    private String codigo;

    @NotBlank(message = "El nombre del cupón es obligatorio")
    @Column(nullable = false, length = 150)
    private String nombre;

    @NotNull(message = "El tipo de descuento es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoDescuento tipo;

    @NotNull(message = "El valor del descuento es obligatorio")
    @DecimalMin(value = "0.01", message = "El valor debe ser mayor a 0")
    @Column(nullable = false)
    private Double valor;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Column(name = "fecha_expiracion")
    private LocalDate fechaExpiracion;

    // null = usos ilimitados
    @Column(name = "limite_usos")
    private Integer limiteUsos;

    @Builder.Default
    @Column(name = "usos_actuales", nullable = false)
    private int usosActuales = 0;

    // Monto mínimo del carrito para aplicar el cupón (0 = sin mínimo)
    @Builder.Default
    @Column(name = "monto_minimo", nullable = false)
    private Double montoMinimo = 0.0;

    @Builder.Default
    @Column(nullable = false)
    private boolean activo = true;

    /** Monto de descuento sobre el total del carrito. */
    public double calcularDescuento(double totalCarrito) {
        if (tipo == TipoDescuento.PORCENTAJE) {
            return totalCarrito * (valor / 100.0);
        }
        return Math.min(valor, totalCarrito);
    }

    /** Verifica validez del cupón (sin revisar el monto mínimo). */
    public boolean estaDisponible() {
        LocalDate hoy = LocalDate.now();
        boolean noExpirado = fechaExpiracion == null || !hoy.isAfter(fechaExpiracion);
        boolean noAgotado = limiteUsos == null || usosActuales < limiteUsos;
        return activo && noExpirado && noAgotado;
    }
}
