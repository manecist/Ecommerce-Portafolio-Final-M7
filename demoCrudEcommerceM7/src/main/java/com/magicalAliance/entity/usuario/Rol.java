package com.magicalAliance.entity.usuario;

import jakarta.persistence.*;
import lombok.*;

// entidad
@Entity
// tabla con nombre
@Table(name = "roles")
// constructor vacio
@NoArgsConstructor
// constructor
@AllArgsConstructor
@Getter
@Setter
// crea objetos de forma segura
@Builder
public class Rol {

    @Id
    // id autoincrementable
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nombre;
}
