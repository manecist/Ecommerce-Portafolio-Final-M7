package com.magicalAliance.dto.usuario;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegistroDTO {

    // --- DATOS DE CUENTA (USUARIO) ---
    private Long id;

    @Email(message = "El formato del email no es válido")
    @NotBlank(message = "El email de acceso es obligatorio")
    private String email;

    // Sin @Pattern aquí: la validación de formato y obligatoriedad se maneja en el service
    // porque este DTO se usa tanto para creación (obligatoria) como para edición (opcional)
    private String password;

    // --- DATOS DE IDENTIDAD (CLIENTE) ---
    @NotBlank(message = "El RUT es obligatorio")
    private String rut;

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    @NotBlank(message = "El apellido es obligatorio")
    private String apellido;

    private String telefono;

    @NotNull(message = "La fecha de nacimiento es obligatoria")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate fechaNacimiento;

    private Long idRol;

    // --- DATOS DE DIRECCIÓN (Vínculo con DireccionCliente) ---
    @NotBlank(message = "El país es obligatorio")
    private String pais;

    @NotBlank(message = "La región o estado es obligatoria")
    private String estadoRegion;

    @NotBlank(message = "La ciudad es obligatoria")
    private String ciudad;

    @NotBlank(message = "La dirección de morada es obligatoria")
    private String direccion;

    private String codigoPostal;

    private boolean esPrincipal;
}