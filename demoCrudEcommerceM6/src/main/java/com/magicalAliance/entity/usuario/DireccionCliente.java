package com.magicalAliance.entity.usuario;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "direcciones_cliente")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DireccionCliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String direccion;

    @Column(nullable = false)
    private String ciudad;

    @Column(name = "estado_region")
    private String estadoRegion;

    @Column(nullable = false)
    private String pais;

    @Column(name = "codigo_postal")
    private String codigoPostal;

    @Column(name = "es_principal")
    private boolean esPrincipal = false;

    // Relación Muchos a Uno: Muchas direcciones pueden pertenecer a un mismo Cliente (RUT)
    // se carga la informacion solo si lo necesita el cliente
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_rut", referencedColumnName = "rut")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Cliente cliente;
}