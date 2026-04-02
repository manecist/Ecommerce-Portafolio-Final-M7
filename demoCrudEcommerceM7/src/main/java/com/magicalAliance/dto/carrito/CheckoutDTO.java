package com.magicalAliance.dto.carrito;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckoutDTO {

    // ID del carrito activo (hidden field en el form)
    private Long carritoId;

    // ── Datos de contacto ─────────────────────────────────────────────────────
    @NotBlank(message = "El nombre de contacto es obligatorio")
    private String nombreContacto;

    @NotBlank(message = "El email de contacto es obligatorio")
    @Email(message = "Debe ingresar un email válido")
    private String emailContacto;

    private String telefonoContacto;

    // ── Dirección de entrega (campos separados, igual que DireccionCliente) ───
    @NotBlank(message = "La calle y número son obligatorios")
    private String calle;

    @NotBlank(message = "La ciudad es obligatoria")
    private String ciudad;

    @NotBlank(message = "La región o estado es obligatorio")
    private String estadoRegion;

    @NotBlank(message = "El país es obligatorio")
    private String pais;

    private String codigoPostal;

    // ── Opcional ──────────────────────────────────────────────────────────────
    private String notasPedido;

    // Helper: construye la dirección snapshot para guardar en el Pedido
    public String buildDireccionEntrega() {
        StringBuilder sb = new StringBuilder();
        sb.append(calle).append(", ").append(ciudad);
        if (estadoRegion != null && !estadoRegion.isBlank()) {
            sb.append(", ").append(estadoRegion);
        }
        sb.append(", ").append(pais);
        if (codigoPostal != null && !codigoPostal.isBlank()) {
            sb.append(" (CP ").append(codigoPostal).append(")");
        }
        return sb.toString();
    }
}