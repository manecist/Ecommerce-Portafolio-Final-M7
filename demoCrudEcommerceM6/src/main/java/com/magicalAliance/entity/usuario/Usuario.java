package com.magicalAliance.entity.usuario;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDate;

@Entity
@Table(name = "usuarios")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // columna unica y no nula
    @Column(unique = true, nullable = false)
    @Email(message = "El formato del email no es valido")
    private String email;

    @Column(nullable = false)
    private String password;

    @Builder.Default
    @Column(name = "fecha_registro")
    private LocalDate fechaRegistro = LocalDate.now();

    //relacion muchos a uno y que cargue todo altiro
    @ManyToOne(fetch = FetchType.EAGER)
    // nombre de su fk
    @JoinColumn(name = "rol_id")
    private Rol rol;

    // relacion con el cliente uno a uno y se elimine el registro cliente y usuario
    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "cliente_rut", referencedColumnName = "rut")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Cliente cliente;
}
