package com.magicalAliance.entity.producto;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.springframework.format.annotation.NumberFormat;

@Entity
@Table(name = "productos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_producto")
    private Long id;

    @NotBlank(message = "El nombre del producto es obligatorio")
    @Column(name = "nombre_producto", nullable = false, length = 150)
    private String nombre;

    @Column(name = "descripcion_producto", columnDefinition = "TEXT")
    private String descripcion;

    @Min(value = 0, message = "El precio no puede ser negativo")
    @NumberFormat(style = NumberFormat.Style.CURRENCY)
    @Column(name = "precio_producto", nullable = false)
    private Double precio = 0.0;

    @Min(value = 0, message = "El stock no puede ser negativo")
    @Column(name = "stock_producto", nullable = false)
    private Integer stock = 0;

    @Column(name = "imagen_producto")
    private String imagen;

    // Relación con Subcategoría
    // Usamos ManyToOne porque muchos productos pertenecen a una subcategoría
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_subcategoria", nullable = false)
    private Subcategoria subcategoria;
}