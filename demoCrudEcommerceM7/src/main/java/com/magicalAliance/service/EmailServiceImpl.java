package com.magicalAliance.service;

import com.magicalAliance.entity.Contacto;
import com.magicalAliance.entity.pedido.EstadoPedido;
import com.magicalAliance.entity.pedido.ItemPedido;
import com.magicalAliance.entity.pedido.Pedido;
import com.magicalAliance.entity.usuario.Usuario;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements IEmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String remitente;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    // ── Suscripción ───────────────────────────────────────────────────────────

    @Override
    public void enviarConfirmacionSuscripcion(String destinatario) {
        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");

            helper.setFrom(remitente);
            helper.setTo(destinatario);
            helper.setSubject("✨ ¡Bienvenida/o a la Alianza Mágica!");

            String bajaUrl = baseUrl + "/subscribir/baja?email=" + destinatario;
            String catalogoUrl = baseUrl + "/productos";
            String cuerpo = """
                    <div style="font-family: Arial, sans-serif; max-width: 600px; margin: auto; background-color: #1a1a2e; color: #ffffff; border-radius: 12px; overflow: hidden;">
                        <div style="background: linear-gradient(135deg, #f480ff, #9b59b6); padding: 30px; text-align: center;">
                            <h1 style="margin: 0; font-size: 26px; color: #ffffff;">✨ Magical Alliance</h1>
                            <p style="margin: 8px 0 0; font-size: 14px; color: #ffe0ff;">Tu comunidad mágica de belleza</p>
                        </div>
                        <div style="padding: 30px;">
                            <h2 style="color: #f480ff;">¡Gracias por unirte!</h2>
                            <p style="color: #cccccc; line-height: 1.6;">
                                A partir de ahora serás de las primeras en enterarte de nuestras
                                <strong style="color: #f480ff;">promociones exclusivas</strong>,
                                nuevos productos y novedades del mundo mágico.
                            </p>
                            <div style="margin: 25px 0; text-align: center;">
                                <a href="%s" style="background: linear-gradient(135deg, #f480ff, #9b59b6); color: #ffffff; text-decoration: none; padding: 12px 30px; border-radius: 25px; font-weight: bold; font-size: 15px;">
                                    Visitar la tienda ✨
                                </a>
                            </div>
                            <p style="color: #888888; font-size: 12px; text-align: center; margin-top: 30px;">
                                Si no solicitaste esta suscripción o deseas darte de baja, puedes hacerlo
                                <a href="%s" style="color: #f480ff;">aquí</a>.
                            </p>
                        </div>
                        <div style="background-color: #111; padding: 15px; text-align: center;">
                            <p style="margin: 0; color: #666666; font-size: 12px;">© 2026 Magical Alliance — Todos los derechos reservados.</p>
                        </div>
                    </div>
                    """.formatted(catalogoUrl, bajaUrl);

            helper.setText(cuerpo, true);
            mailSender.send(mensaje);

        } catch (MessagingException e) {
            throw new RuntimeException("Error al enviar el correo de confirmación: " + e.getMessage(), e);
        }
    }

    // ── Confirmación de pedido ────────────────────────────────────────────────

    @Override
    public void enviarConfirmacionPedido(Pedido pedido) {
        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");

            helper.setFrom(remitente);
            helper.setTo(pedido.getEmailContacto());
            helper.setSubject("✨ Pedido #" + pedido.getId() + " recibido — Magical Alliance");

            helper.setText(buildHtmlConfirmacionPedido(pedido), true);
            mailSender.send(mensaje);

        } catch (MessagingException e) {
            // Fallo silencioso para no bloquear la confirmación del pedido
            System.err.println("[EMAIL] Error al enviar confirmación del pedido #" + pedido.getId() + ": " + e.getMessage());
        }
    }

    // ── Actualización de estado ───────────────────────────────────────────────

    @Override
    public void enviarActualizacionEstadoPedido(Pedido pedido) {
        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");

            helper.setFrom(remitente);
            helper.setTo(pedido.getEmailContacto());
            helper.setSubject("🔔 Actualización de tu pedido #" + pedido.getId() + " — Magical Alliance");

            helper.setText(buildHtmlActualizacionEstado(pedido), true);
            mailSender.send(mensaje);

        } catch (MessagingException e) {
            System.err.println("[EMAIL] Error al enviar actualización de estado del pedido #" + pedido.getId() + ": " + e.getMessage());
        }
    }

    // ── Builders de HTML ──────────────────────────────────────────────────────

    private String buildHtmlConfirmacionPedido(Pedido pedido) {
        StringBuilder items = new StringBuilder();
        for (ItemPedido item : pedido.getItems()) {
            items.append("""
                    <tr>
                        <td style="padding: 10px; border-bottom: 1px solid #2a2a4a; color: #dddddd;">%s</td>
                        <td style="padding: 10px; border-bottom: 1px solid #2a2a4a; color: #dddddd; text-align:center;">%d</td>
                        <td style="padding: 10px; border-bottom: 1px solid #2a2a4a; color: #f480ff; text-align:right; font-weight:bold;">$ %s</td>
                    </tr>
                    """.formatted(
                    item.getNombreProducto(),
                    item.getCantidad(),
                    formatMonto(item.getSubtotal())
            ));
        }

        return """
                <div style="font-family: Arial, sans-serif; max-width: 620px; margin: auto; background-color: #1a1a2e; border-radius: 14px; overflow: hidden;">
                    <div style="background: linear-gradient(135deg, #6907ab, #f480ff); padding: 28px; text-align: center;">
                        <h1 style="margin:0; color:#fff; font-size:24px;">✨ Magical Alliance</h1>
                        <p style="margin:6px 0 0; color:#ffe0ff; font-size:13px;">Confirmación de pedido</p>
                    </div>

                    <div style="padding: 28px;">
                        <h2 style="color:#f480ff; margin-top:0;">¡Pedido #%d recibido!</h2>
                        <p style="color:#cccccc; line-height:1.6;">
                            Hola <strong style="color:#fff;">%s</strong>, hemos recibido tu pedido correctamente.
                            Pronto te contactaremos para coordinar el envío y el pago.
                        </p>

                        <table style="width:100%%; border-collapse:collapse; margin: 20px 0;">
                            <thead>
                                <tr style="background-color: #2a1a4e;">
                                    <th style="padding:10px; text-align:left; color:#f480ff; font-size:13px;">Producto</th>
                                    <th style="padding:10px; text-align:center; color:#f480ff; font-size:13px;">Cant.</th>
                                    <th style="padding:10px; text-align:right; color:#f480ff; font-size:13px;">Subtotal</th>
                                </tr>
                            </thead>
                            <tbody>
                                %s
                            </tbody>
                        </table>

                        <div style="background-color:#2a1a4e; border-radius:10px; padding:16px; margin-bottom:20px;">
                            <table style="width:100%%; border-collapse:collapse;">
                                <tr>
                                    <td style="color:#aaa; padding:4px 0;">Neto</td>
                                    <td style="color:#ddd; text-align:right; padding:4px 0;">$ %s</td>
                                </tr>
                                <tr>
                                    <td style="color:#aaa; padding:4px 0;">IVA (19%%)</td>
                                    <td style="color:#ddd; text-align:right; padding:4px 0;">$ %s</td>
                                </tr>
                                <tr style="border-top:1px solid #4a3a7e;">
                                    <td style="color:#f480ff; font-weight:bold; font-size:16px; padding-top:10px;">Total</td>
                                    <td style="color:#f480ff; font-weight:bold; font-size:16px; text-align:right; padding-top:10px;">$ %s</td>
                                </tr>
                            </table>
                        </div>

                        <div style="background-color:#2a1a4e; border-radius:10px; padding:16px; margin-bottom:16px;">
                            <p style="color:#f480ff; font-weight:bold; margin:0 0 8px;">📦 Dirección de entrega</p>
                            <p style="color:#cccccc; margin:0;">%s</p>
                        </div>

                        <div style="background-color:#1a1040; border-radius:10px; padding:16px; border-left:3px solid #f480ff;">
                            <p style="color:#f480ff; font-weight:bold; margin:0 0 8px;">ℹ️ ¿Necesitas cancelar o devolver?</p>
                            <p style="color:#cccccc; margin:0; font-size:13px; line-height:1.6;">
                                Si deseas <strong style="color:#fff;">cancelar tu pedido</strong> o solicitar una
                                <strong style="color:#fff;">devolución</strong>, puedes gestionarlo directamente
                                desde <strong style="color:#f480ff;">Mis Pedidos</strong> en nuestra página
                                (es necesario tener una cuenta registrada).<br><br>
                                Para cualquier otra consulta o problema, contáctanos a través del
                                <strong style="color:#f480ff;">Panel de Contacto</strong> disponible en el sitio.
                            </p>
                        </div>
                    </div>

                    <div style="background-color:#111; padding:15px; text-align:center;">
                        <p style="margin:0; color:#666; font-size:12px;">© 2026 Magical Alliance — Todos los derechos reservados.</p>
                    </div>
                </div>
                """.formatted(
                pedido.getId(),
                pedido.getNombreParaMostrar(),
                items.toString(),
                formatMonto(pedido.getSubtotal()),
                formatMonto(pedido.getIva()),
                formatMonto(pedido.getTotal()),
                pedido.getDireccionEntrega()
        );
    }

    private String buildHtmlActualizacionEstado(Pedido pedido) {
        String bloqueInfo = buildBloqueInfoEstado(pedido.getEstado());

        return ("""
                <div style="font-family: Arial, sans-serif; max-width: 620px; margin: auto; background-color: #1a1a2e; border-radius: 14px; overflow: hidden;">
                    <div style="background: linear-gradient(135deg, #6907ab, #f480ff); padding: 28px; text-align: center;">
                        <h1 style="margin:0; color:#fff; font-size:24px;">✨ Magical Alliance</h1>
                        <p style="margin:6px 0 0; color:#ffe0ff; font-size:13px;">Actualización de tu pedido</p>
                    </div>

                    <div style="padding: 28px;">
                        <h2 style="color:#f480ff; margin-top:0;">Pedido #%d actualizado</h2>
                        <p style="color:#cccccc; line-height:1.6;">
                            Hola <strong style="color:#fff;">%s</strong>, el estado de tu pedido ha sido actualizado.
                        </p>

                        <div style="background-color:#2a1a4e; border-radius:10px; padding:20px; text-align:center; margin:20px 0;">
                            <p style="color:#aaa; margin:0 0 8px; font-size:13px;">Estado actual</p>
                            <span style="background: linear-gradient(135deg, #6907ab, #f480ff); color:#fff; padding:8px 24px; border-radius:20px; font-weight:bold; font-size:16px;">
                                %s
                            </span>
                        </div>

                        """).formatted(pedido.getId(), pedido.getNombreParaMostrar(), pedido.getEstado().name())
                + bloqueInfo
                + """

                    </div>

                    <div style="background-color:#111; padding:15px; text-align:center;">
                        <p style="margin:0; color:#666; font-size:12px;">© 2026 Magical Alliance — Todos los derechos reservados.</p>
                    </div>
                </div>
                """;
    }

    private String buildBloqueInfoEstado(EstadoPedido estado) {
        return switch (estado) {
            case PENDIENTE, CONFIRMADO, EN_PREPARACION -> """
                    <div style="background-color:#1a1040; border-radius:10px; padding:16px; border-left:3px solid #f480ff;">
                        <p style="color:#f480ff; font-weight:bold; margin:0 0 8px;">ℹ️ ¿Necesitas cancelar tu pedido?</p>
                        <p style="color:#cccccc; margin:0; font-size:13px; line-height:1.6;">
                            Puedes cancelar tu pedido directamente desde
                            <strong style="color:#f480ff;">Mis Pedidos</strong> en nuestra página
                            (es necesario tener una cuenta registrada).<br><br>
                            Para cualquier otra consulta o problema, contáctanos a través del
                            <strong style="color:#f480ff;">Panel de Contacto</strong> disponible en el sitio.
                        </p>
                    </div>
                    """;
            case ENVIADO -> """
                    <div style="background-color:#1a1040; border-radius:10px; padding:16px; border-left:3px solid #f480ff;">
                        <p style="color:#f480ff; font-weight:bold; margin:0 0 8px;">🎉 ¡Tu pedido está en camino!</p>
                        <p style="color:#cccccc; margin:0; font-size:13px; line-height:1.6;">
                            ¡Esperamos que disfrutes tu compra! Si tuvieras algún inconveniente al recibirlo,
                            no dudes en contactarnos a través del
                            <strong style="color:#f480ff;">Panel de Contacto</strong> y con gusto coordinaremos
                            una devolución o encontraremos la mejor solución para ti. 🌙
                        </p>
                    </div>
                    """;
            case ENTREGADO -> """
                    <div style="background-color:#1a1040; border-radius:10px; padding:16px; border-left:3px solid #f480ff;">
                        <p style="color:#f480ff; font-weight:bold; margin:0 0 8px;">🌟 ¡Pedido entregado!</p>
                        <p style="color:#cccccc; margin:0; font-size:13px; line-height:1.6;">
                            ¡Esperamos que disfrutes al máximo tu pedido! Ha sido un placer acompañarte en esta
                            aventura mágica. Si tienes alguna consulta o necesitas ayuda, no dudes en contactarnos
                            a través del <strong style="color:#f480ff;">Panel de Contacto</strong> disponible en el sitio.
                        </p>
                    </div>
                    """;
            case CANCELADO -> """
                    <div style="background-color:#1a1040; border-radius:10px; padding:16px; border-left:3px solid #f480ff;">
                        <p style="color:#f480ff; font-weight:bold; margin:0 0 8px;">💜 Pedido cancelado</p>
                        <p style="color:#cccccc; margin:0; font-size:13px; line-height:1.6;">
                            Lamentamos los inconvenientes generados. Esperamos que te encuentres bien y recuerda
                            que siempre puedes volver a comprar en
                            <strong style="color:#f480ff;">Magical Alliance</strong>, ¡estaremos encantadas
                            de atenderte! Si necesitas comunicarte con nosotros, estamos disponibles a través del
                            <strong style="color:#f480ff;">Panel de Contacto</strong>.
                        </p>
                    </div>
                    """;
            case DEVOLUCION_SOLICITADA -> """
                    <div style="background-color:#1a1040; border-radius:10px; padding:16px; border-left:3px solid #f480ff;">
                        <p style="color:#f480ff; font-weight:bold; margin:0 0 8px;">🔄 Solicitud de devolución recibida</p>
                        <p style="color:#cccccc; margin:0; font-size:13px; line-height:1.6;">
                            Hemos recibido tu solicitud y lamentamos los inconvenientes. Pronto nos pondremos en
                            contacto para coordinar el proceso de devolución. Recuerda que siempre puedes volver a
                            comprar en <strong style="color:#f480ff;">Magical Alliance</strong> y que estamos
                            aquí para ayudarte en lo que necesites. 💜
                        </p>
                    </div>
                    """;
            case DEVOLUCION_REALIZADA -> """
                    <div style="background-color:#1a1040; border-radius:10px; padding:16px; border-left:3px solid #f480ff;">
                        <p style="color:#f480ff; font-weight:bold; margin:0 0 8px;">✅ Devolución completada</p>
                        <p style="color:#cccccc; margin:0; font-size:13px; line-height:1.6;">
                            Tu devolución ha sido procesada exitosamente. Lamentamos los inconvenientes y esperamos
                            que te encuentres bien. Recuerda que siempre puedes volver a comprar en
                            <strong style="color:#f480ff;">Magical Alliance</strong>, ¡estaremos felices de verte
                            de nuevo! Si tienes alguna consulta, contáctanos en el
                            <strong style="color:#f480ff;">Panel de Contacto</strong>. 💜
                        </p>
                    </div>
                    """;
            default -> """
                    <div style="background-color:#1a1040; border-radius:10px; padding:16px; border-left:3px solid #f480ff;">
                        <p style="color:#cccccc; margin:0; font-size:13px; line-height:1.6;">
                            Si tienes alguna consulta o necesitas ayuda, contáctanos a través del
                            <strong style="color:#f480ff;">Panel de Contacto</strong> disponible en el sitio.
                        </p>
                    </div>
                    """;
        };
    }

    // ── Confirmación de baja de suscripción ──────────────────────────────────

    @Override
    public void enviarConfirmacionBaja(String email) {
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
            helper.setFrom(remitente);
            helper.setTo(email);
            helper.setSubject("💜 Baja de suscripción confirmada — Magical Alliance");

            String catalogoUrl = baseUrl + "/productos";
            String cuerpo = """
                    <div style="font-family: Arial, sans-serif; max-width: 600px; margin: auto; background-color: #1a1a2e; border-radius: 12px; overflow: hidden;">
                        <div style="background: linear-gradient(135deg, #6907ab, #f480ff); padding: 28px; text-align: center;">
                            <h1 style="margin:0; color:#fff; font-size:24px;">✨ Magical Alliance</h1>
                            <p style="margin:6px 0 0; color:#ffe0ff; font-size:13px;">Baja de suscripción</p>
                        </div>
                        <div style="padding: 28px;">
                            <h2 style="color:#f480ff; margin-top:0;">Baja confirmada 💜</h2>
                            <p style="color:#cccccc; line-height:1.6;">
                                Tu baja de suscripción ha sido procesada exitosamente. Lamentamos verte partir,
                                pero respetamos tu decisión. Ya no recibirás nuestras comunicaciones de marketing.
                            </p>
                            <div style="background-color:#1a1040; border-radius:10px; padding:16px; margin:20px 0; border-left:3px solid #f480ff;">
                                <p style="color:#cccccc; margin:0; font-size:13px; line-height:1.6;">
                                    Recuerda que siempre puedes volver a visitarnos y suscribirte cuando lo desees.
                                    ¡Las puertas de <strong style="color:#f480ff;">Magical Alliance</strong> siempre están abiertas para ti! 🌙
                                </p>
                            </div>
                            <div style="text-align:center; margin-top:20px;">
                                <a href="%s" style="background: linear-gradient(135deg, #6907ab, #f480ff); color:#fff; text-decoration:none; padding:10px 28px; border-radius:25px; font-weight:bold; font-size:14px; display:inline-block;">
                                    Visitar la tienda ✨
                                </a>
                            </div>
                        </div>
                        <div style="background-color:#111; padding:15px; text-align:center;">
                            <p style="margin:0; color:#666; font-size:12px;">© 2026 Magical Alliance — Todos los derechos reservados.</p>
                        </div>
                    </div>
                    """.formatted(catalogoUrl);

            helper.setText(cuerpo, true);
            mailSender.send(msg);
        } catch (MessagingException e) {
            System.err.println("[EMAIL] Error al enviar confirmación de baja a " + email + ": " + e.getMessage());
        }
    }

    // ── Bienvenida al registrarse ─────────────────────────────────────────────

    @Override
    public void enviarBienvenidaRegistro(Usuario usuario) {
        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");

            helper.setFrom(remitente);
            helper.setTo(usuario.getEmail());
            helper.setSubject("✨ ¡Bienvenida/o a Magical Alliance!");

            String nombre = usuario.getCliente() != null
                    ? usuario.getCliente().getNombre()
                    : usuario.getEmail();

            String cuerpo = """
                    <div style="font-family: Arial, sans-serif; max-width: 620px; margin: auto; background-color: #1a1a2e; border-radius: 14px; overflow: hidden;">
                        <div style="background: linear-gradient(135deg, #6907ab, #f480ff); padding: 28px; text-align: center;">
                            <h1 style="margin:0; color:#fff; font-size:24px;">✨ Magical Alliance</h1>
                            <p style="margin:6px 0 0; color:#ffe0ff; font-size:13px;">Tu comunidad mágica de belleza</p>
                        </div>

                        <div style="padding: 28px;">
                            <h2 style="color:#f480ff; margin-top:0;">¡Hola, %s! 🌙</h2>
                            <p style="color:#cccccc; line-height:1.6;">
                                Tu cuenta ha sido creada exitosamente en <strong style="color:#fff;">Magical Alliance</strong>.
                                A partir de ahora formas parte de nuestra comunidad mágica de belleza.
                            </p>
                            <p style="color:#cccccc; line-height:1.6;">
                                Desde tu cuenta podrás revisar el historial de tus pedidos, gestionar tus direcciones,
                                cancelar pedidos antes de su envío y mucho más. ✨
                            </p>

                            <div style="background-color:#2a1a4e; border-radius:10px; padding:20px; margin:24px 0; text-align:center;">
                                <p style="color:#aaa; margin:0 0 12px; font-size:13px;">¡Empieza tu aventura mágica!</p>
                                <a href="#" style="background: linear-gradient(135deg, #6907ab, #f480ff); color:#fff; text-decoration:none; padding:12px 32px; border-radius:25px; font-weight:bold; font-size:15px; display:inline-block;">
                                    Explorar el catálogo ✨
                                </a>
                            </div>

                            <div style="background-color:#1a1040; border-radius:10px; padding:16px; border-left:3px solid #f480ff;">
                                <p style="color:#cccccc; margin:0; font-size:13px; line-height:1.6;">
                                    Si no creaste esta cuenta o tienes alguna consulta, contáctanos a través del
                                    <strong style="color:#f480ff;">Panel de Contacto</strong> disponible en el sitio.
                                </p>
                            </div>
                        </div>

                        <div style="background-color:#111; padding:15px; text-align:center;">
                            <p style="margin:0; color:#666; font-size:12px;">© 2026 Magical Alliance — Todos los derechos reservados.</p>
                        </div>
                    </div>
                    """.formatted(nombre);

            helper.setText(cuerpo, true);
            mailSender.send(mensaje);

        } catch (MessagingException e) {
            System.err.println("[EMAIL] Error al enviar bienvenida al usuario " + usuario.getEmail() + ": " + e.getMessage());
        }
    }

    // ── Notificación de contacto ──────────────────────────────────────────────

    @Override
    public void enviarNotificacionContacto(Contacto contacto) {
        // 1. Confirmación al remitente
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
            helper.setFrom(remitente);
            helper.setTo(contacto.getEmail());
            helper.setSubject("✨ Hemos recibido tu mensaje — Magical Alliance");
            helper.setText(buildHtmlConfirmacionContacto(contacto), true);
            mailSender.send(msg);
        } catch (MessagingException e) {
            System.err.println("[EMAIL] Error al enviar confirmación de contacto a " + contacto.getEmail() + ": " + e.getMessage());
        }

        // 2. Notificación al administrador
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
            helper.setFrom(remitente);
            helper.setTo(remitente);
            helper.setSubject("🔔 Nuevo mensaje de contacto de " + contacto.getNombre() + " — Magical Alliance");
            helper.setText(buildHtmlNotificacionAdminContacto(contacto), true);
            mailSender.send(msg);
        } catch (MessagingException e) {
            System.err.println("[EMAIL] Error al enviar notificación de contacto al admin: " + e.getMessage());
        }
    }

    private String buildHtmlConfirmacionContacto(Contacto contacto) {
        return """
                <div style="font-family: Arial, sans-serif; max-width: 620px; margin: auto; background-color: #1a1a2e; border-radius: 14px; overflow: hidden;">
                    <div style="background: linear-gradient(135deg, #6907ab, #f480ff); padding: 28px; text-align: center;">
                        <h1 style="margin:0; color:#fff; font-size:24px;">✨ Magical Alliance</h1>
                        <p style="margin:6px 0 0; color:#ffe0ff; font-size:13px;">Confirmación de mensaje</p>
                    </div>

                    <div style="padding: 28px;">
                        <h2 style="color:#f480ff; margin-top:0;">¡Hola, %s!</h2>
                        <p style="color:#cccccc; line-height:1.6;">
                            Hemos recibido tu mensaje correctamente. Nuestro equipo lo revisará y
                            nos pondremos en contacto contigo a la brevedad. 🌙
                        </p>

                        <div style="background-color:#2a1a4e; border-radius:10px; padding:16px; margin:20px 0;">
                            <p style="color:#f480ff; font-weight:bold; margin:0 0 8px;">📩 Tu mensaje:</p>
                            <p style="color:#cccccc; margin:0; font-size:13px; line-height:1.6; font-style:italic;">"%s"</p>
                        </div>

                        <div style="background-color:#1a1040; border-radius:10px; padding:16px; border-left:3px solid #f480ff;">
                            <p style="color:#cccccc; margin:0; font-size:13px; line-height:1.6;">
                                Si tienes alguna consulta adicional urgente, puedes responder este correo
                                o volver al <strong style="color:#f480ff;">Panel de Contacto</strong> en nuestro sitio.
                            </p>
                        </div>
                    </div>

                    <div style="background-color:#111; padding:15px; text-align:center;">
                        <p style="margin:0; color:#666; font-size:12px;">© 2026 Magical Alliance — Todos los derechos reservados.</p>
                    </div>
                </div>
                """.formatted(contacto.getNombre(), contacto.getMensaje());
    }

    private String buildHtmlNotificacionAdminContacto(Contacto contacto) {
        return """
                <div style="font-family: Arial, sans-serif; max-width: 620px; margin: auto; background-color: #1a1a2e; border-radius: 14px; overflow: hidden;">
                    <div style="background: linear-gradient(135deg, #6907ab, #f480ff); padding: 28px; text-align: center;">
                        <h1 style="margin:0; color:#fff; font-size:24px;">✨ Magical Alliance</h1>
                        <p style="margin:6px 0 0; color:#ffe0ff; font-size:13px;">Nuevo mensaje de contacto</p>
                    </div>

                    <div style="padding: 28px;">
                        <h2 style="color:#f480ff; margin-top:0;">🔔 Nuevo mensaje recibido</h2>

                        <div style="background-color:#2a1a4e; border-radius:10px; padding:16px; margin-bottom:16px;">
                            <table style="width:100%%; border-collapse:collapse;">
                                <tr>
                                    <td style="color:#aaa; font-size:13px; padding:4px 0; width:30%%;">Nombre:</td>
                                    <td style="color:#fff; font-size:13px; padding:4px 0; font-weight:bold;">%s</td>
                                </tr>
                                <tr>
                                    <td style="color:#aaa; font-size:13px; padding:4px 0;">Email:</td>
                                    <td style="color:#f480ff; font-size:13px; padding:4px 0;">%s</td>
                                </tr>
                            </table>
                        </div>

                        <div style="background-color:#2a1a4e; border-radius:10px; padding:16px;">
                            <p style="color:#f480ff; font-weight:bold; margin:0 0 8px;">📩 Mensaje:</p>
                            <p style="color:#cccccc; margin:0; font-size:13px; line-height:1.6;">%s</p>
                        </div>
                    </div>

                    <div style="background-color:#111; padding:15px; text-align:center;">
                        <p style="margin:0; color:#666; font-size:12px;">© 2026 Magical Alliance — Panel Administrativo</p>
                    </div>
                </div>
                """.formatted(contacto.getNombre(), contacto.getEmail(), contacto.getMensaje());
    }

    // ── Confirmación de eliminación de cuenta ────────────────────────────────

    @Override
    public void enviarConfirmacionEliminacionCuenta(Usuario usuario) {
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
            helper.setFrom(remitente);
            helper.setTo(usuario.getEmail());
            helper.setSubject("💜 Hasta pronto — Magical Alliance");

            String nombre = usuario.getCliente() != null ? usuario.getCliente().getNombre() : "Habitante";
            String catalogoUrl = baseUrl + "/productos";

            String cuerpo = """
                    <div style="font-family: Arial, sans-serif; max-width: 600px; margin: auto; background-color: #1a1a2e; border-radius: 12px; overflow: hidden;">
                        <div style="background: linear-gradient(135deg, #6907ab, #f480ff); padding: 28px; text-align: center;">
                            <h1 style="margin:0; color:#fff; font-size:24px;">✨ Magical Alliance</h1>
                            <p style="margin:6px 0 0; color:#ffe0ff; font-size:13px;">Tu comunidad mágica de belleza</p>
                        </div>
                        <div style="padding: 28px;">
                            <h2 style="color:#f480ff; margin-top:0;">Hasta pronto, %s 🌙</h2>
                            <p style="color:#cccccc; line-height:1.7;">
                                Tu cuenta en <strong style="color:#fff;">Magical Alliance</strong> ha sido eliminada exitosamente.
                                Lamentamos tu decisión, pero respetamos y agradecemos haber sido parte de tu aventura mágica.
                            </p>
                            <div style="background-color:#2a1a4e; border-radius:10px; padding:20px; margin:20px 0; text-align:center;">
                                <p style="color:#f480ff; font-size:22px; margin:0 0 8px;">💜</p>
                                <p style="color:#cccccc; font-size:14px; line-height:1.7; margin:0;">
                                    Recuerda que las puertas de <strong style="color:#f480ff;">Magical Alliance</strong>
                                    siempre estarán abiertas para ti.<br>
                                    <strong style="color:#fff;">Siempre serás bienvenida/o</strong> cuando desees regresar. 🌸
                                </p>
                            </div>
                            <div style="background-color:#1a1040; border-radius:10px; padding:16px; border-left:3px solid #f480ff; margin-bottom:20px;">
                                <p style="color:#cccccc; margin:0; font-size:13px; line-height:1.6;">
                                    Si eliminaste tu cuenta por error o cambias de opinión en el futuro,
                                    puedes registrarte nuevamente cuando lo desees. Estaremos felices de recibirte de vuelta. ✨
                                </p>
                            </div>
                            <div style="text-align:center;">
                                <a href="%s" style="background: linear-gradient(135deg, #6907ab, #f480ff); color:#fff; text-decoration:none; padding:10px 28px; border-radius:25px; font-weight:bold; font-size:14px; display:inline-block;">
                                    Visitar la tienda ✨
                                </a>
                            </div>
                        </div>
                        <div style="background-color:#111; padding:15px; text-align:center;">
                            <p style="margin:0; color:#666; font-size:12px;">© 2026 Magical Alliance — Siempre en nuestros corazones.</p>
                        </div>
                    </div>
                    """.formatted(nombre, catalogoUrl);

            helper.setText(cuerpo, true);
            mailSender.send(msg);
        } catch (MessagingException e) {
            System.err.println("[EMAIL] Error al enviar despedida a " + usuario.getEmail() + ": " + e.getMessage());
        }
    }

    // ── Recuperación de contraseña ───────────────────────────────────────────

    @Override
    public void enviarRecuperacionContrasena(String email, String token) {
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
            helper.setFrom(remitente);
            helper.setTo(email);
            helper.setSubject("🔑 Recupera tu contraseña — Magical Alliance");

            String enlace = baseUrl + "/nueva-contrasena?token=" + token;

            String cuerpo = """
                    <div style="font-family: Arial, sans-serif; max-width: 600px; margin: auto; background-color: #1a1a2e; border-radius: 12px; overflow: hidden;">
                        <div style="background: linear-gradient(135deg, #6907ab, #f480ff); padding: 28px; text-align: center;">
                            <h1 style="margin:0; color:#fff; font-size:24px;">✨ Magical Alliance</h1>
                            <p style="margin:6px 0 0; color:#ffe0ff; font-size:13px;">Tu comunidad mágica de belleza</p>
                        </div>
                        <div style="padding: 30px;">
                            <h2 style="color:#f480ff; margin-top:0;">Recupera tu contraseña 🔑</h2>
                            <p style="color:#cccccc; line-height:1.7;">
                                Hemos recibido una solicitud para restablecer la contraseña de tu cuenta.
                                Haz clic en el botón a continuación para crear una nueva contraseña.
                            </p>
                            <p style="color:#aaaaaa; font-size:13px;">
                                Este enlace es válido por <strong style="color:#f480ff;">1 hora</strong>.
                                Si no solicitaste este cambio, puedes ignorar este correo.
                            </p>
                            <div style="text-align:center; margin: 28px 0;">
                                <a href="%s"
                                   style="background: linear-gradient(135deg, #6907ab, #f480ff); color: #ffffff;
                                          text-decoration: none; padding: 14px 36px; border-radius: 30px;
                                          font-weight: bold; font-size: 15px; display: inline-block;">
                                    🔑 Restablecer contraseña
                                </a>
                            </div>
                            <p style="color:#666666; font-size:12px; text-align:center; margin-top:24px;">
                                Si el botón no funciona, copia y pega este enlace en tu navegador:<br>
                                <a href="%s" style="color:#f480ff; word-break:break-all;">%s</a>
                            </p>
                        </div>
                        <div style="background-color:#111; padding:15px; text-align:center;">
                            <p style="margin:0; color:#666; font-size:12px;">© 2026 Magical Alliance — Todos los derechos reservados.</p>
                        </div>
                    </div>
                    """.formatted(enlace, enlace, enlace);

            helper.setText(cuerpo, true);
            mailSender.send(msg);
        } catch (MessagingException e) {
            System.err.println("[EMAIL] Error al enviar recuperación de contraseña a " + email + ": " + e.getMessage());
        }
    }

    private String formatMonto(Double monto) {
        if (monto == null) return "0";
        long redondeado = Math.round(monto);
        // Formato con punto como separador de miles: 1.234.567
        String s = String.valueOf(redondeado);
        StringBuilder resultado = new StringBuilder();
        int inicio = s.length() % 3;
        if (inicio > 0) resultado.append(s, 0, inicio);
        for (int i = inicio; i < s.length(); i += 3) {
            if (resultado.length() > 0) resultado.append('.');
            resultado.append(s, i, i + 3);
        }
        return resultado.toString();
    }
}