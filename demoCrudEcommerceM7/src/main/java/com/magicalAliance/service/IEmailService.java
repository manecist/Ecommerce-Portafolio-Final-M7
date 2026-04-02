package com.magicalAliance.service;

import com.magicalAliance.entity.Contacto;
import com.magicalAliance.entity.pedido.Pedido;
import com.magicalAliance.entity.usuario.Usuario;

public interface IEmailService {

    void enviarConfirmacionSuscripcion(String destinatario);

    /**
     * Envía confirmación del pedido al cliente cuando éste confirma el checkout.
     */
    void enviarConfirmacionPedido(Pedido pedido);

    /**
     * Notifica al cliente cuando el admin cambia el estado del pedido.
     */
    void enviarActualizacionEstadoPedido(Pedido pedido);

    /**
     * Envía un correo de bienvenida al nuevo usuario registrado.
     */
    void enviarBienvenidaRegistro(Usuario usuario);

    /**
     * Envía confirmación al remitente del formulario de contacto
     * y notifica al administrador del nuevo mensaje recibido.
     */
    void enviarNotificacionContacto(Contacto contacto);

    /**
     * Envía confirmación de baja de suscripción al usuario.
     */
    void enviarConfirmacionBaja(String email);

    /**
     * Envía correo de despedida cuando el usuario elimina su propia cuenta.
     */
    void enviarConfirmacionEliminacionCuenta(Usuario usuario);

    /**
     * Envía enlace de recuperación de contraseña al correo indicado.
     */
    void enviarRecuperacionContrasena(String email, String token);
}