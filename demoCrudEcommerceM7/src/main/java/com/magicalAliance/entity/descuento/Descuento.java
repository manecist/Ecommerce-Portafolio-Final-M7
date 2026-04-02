package com.magicalAliance.entity.descuento;

import com.magicalAliance.entity.producto.Categoria;
import com.magicalAliance.entity.producto.Producto;
import com.magicalAliance.entity.producto.Subcategoria;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Entity
@Table(name = "descuentos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Descuento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre del descuento es obligatorio")
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

    @NotNull(message = "El alcance del descuento es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AlcanceDescuento alcance;

    // Solo uno de estos tres es no-nulo según el alcance
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "categoria_id")
    private Categoria categoria;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "subcategoria_id")
    private Subcategoria subcategoria;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "producto_id")
    private Producto producto;

    @NotNull(message = "La fecha de inicio es obligatoria")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @NotNull(message = "La fecha de fin es obligatoria")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Column(name = "fecha_fin", nullable = false)
    private LocalDate fechaFin;

    @Builder.Default
    @Column(nullable = false)
    private boolean activo = true;

    /** Monto de ahorro que genera este descuento sobre un precio dado. */
    public double calcularAhorro(double precioOriginal) {
        if (tipo == TipoDescuento.PORCENTAJE) {
            return precioOriginal * (valor / 100.0);
        }
        return Math.min(valor, precioOriginal);
    }

    /** Precio final después de aplicar el descuento. Nunca negativo. */
    public double calcularPrecioFinal(double precioOriginal) {
        return Math.max(0, precioOriginal - calcularAhorro(precioOriginal));
    }

    /** Descripción del objetivo al que aplica. */
    public String getObjetivoDescripcion() {
        return switch (alcance) {
            case GLOBAL -> "Todos los productos";
            case CATEGORIA -> categoria != null ? categoria.getNombre() : "—";
            case SUBCATEGORIA -> subcategoria != null ? subcategoria.getNombre() : "—";
            case PRODUCTO -> producto != null ? producto.getNombre() : "—";
        };
    }

    public boolean estaVigente() {
        LocalDate hoy = LocalDate.now();
        return activo && !hoy.isBefore(fechaInicio) && !hoy.isAfter(fechaFin);
    }
}
