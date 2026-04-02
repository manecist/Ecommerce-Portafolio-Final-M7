package com.magicalAliance.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "suscriptores")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Suscriptor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_suscriptor")
    private Long id;

    @Column(name = "email", nullable = false, length = 150, unique = true)
    private String email;

    @Column(name = "fecha_suscripcion", nullable = false)
    private LocalDateTime fechaSuscripcion;

    @PrePersist
    public void prePersist() {
        this.fechaSuscripcion = LocalDateTime.now();
    }
}