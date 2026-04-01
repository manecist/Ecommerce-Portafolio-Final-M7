package com.magicalAliance.entity.producto;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "categorias")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Categoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_categoria")
    private Long id;

    @NotBlank(message = "El nombre de la categoría es obligatorio")
    @Column(name = "nombre_categoria", nullable = false, length = 100)
    private String nombre;

    @Column(name = "imagen_banner")
    private String imagenBanner = "banner-simple.jpg";

    // Una categoría tiene muchas subcategorías
    @Builder.Default
    @OneToMany(mappedBy = "categoria", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Subcategoria> subcategorias = new ArrayList<>();
}