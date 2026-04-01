package com.magicalAliance.controller.pedido;

import com.magicalAliance.entity.pedido.EstadoPedido;
import com.magicalAliance.entity.pedido.Pedido;
import com.magicalAliance.entity.usuario.Usuario;
import com.magicalAliance.exception.MagicalBusinessException;
import com.magicalAliance.repository.usuario.UsuarioRepository;
import com.magicalAliance.service.pedido.IPedidoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

@Controller
public class PedidoController {

    @Autowired private IPedidoService pedidoService;
    @Autowired private UsuarioRepository usuarioRepo;

    // ─── Historial del cliente ────────────────────────────────────────────────

    @GetMapping("/mis-pedidos")
    @PreAuthorize("hasAnyRole('CLIENT', 'ADMIN')")
    public String misPedidos(Authentication auth, Model model) {
        Usuario usuario = usuarioRepo.findByEmail(auth.getName())
                .orElseThrow(() -> new MagicalBusinessException("Usuario no encontrado"));

        List<Pedido> pedidos = pedidoService.listarPorCliente(usuario.getCliente());
        model.addAttribute("pedidos", pedidos);
        return "client/pedidos/mis-pedidos";
    }

    @GetMapping("/mis-pedidos/{id}")
    @PreAuthorize("hasAnyRole('CLIENT', 'ADMIN')")
    public String detallePedido(@PathVariable Long id, Authentication auth, Model model) {
        Pedido pedido = pedidoService.buscarPorId(id)
                .orElseThrow(() -> new MagicalBusinessException("Pedido #" + id + " no encontrado"));

        // Verificar que el pedido pertenece al usuario (o es admin)
        boolean esAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!esAdmin && pedido.getCliente() != null) {
            Usuario usuario = usuarioRepo.findByEmail(auth.getName()).orElse(null);
            if (usuario == null || !pedido.getCliente().getId().equals(usuario.getCliente().getId())) {
                throw new MagicalBusinessException("No tienes permiso para ver este pedido");
            }
        }

        model.addAttribute("pedido", pedido);
        return "client/pedidos/detalle-pedido";
    }

    // ─── Cancelar pedido (cliente) ────────────────────────────────────────────

    @PostMapping("/mis-pedidos/{id}/cancelar")
    @PreAuthorize("hasAnyRole('CLIENT', 'ADMIN')")
    public String cancelarPedido(@PathVariable Long id, Authentication auth, RedirectAttributes flash) {
        try {
            Usuario usuario = usuarioRepo.findByEmail(auth.getName())
                    .orElseThrow(() -> new MagicalBusinessException("Usuario no encontrado"));
            pedidoService.cancelarPedido(id, usuario.getCliente());
            flash.addFlashAttribute("success", "Pedido #" + id + " cancelado correctamente");
        } catch (MagicalBusinessException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/mis-pedidos/" + id;
    }

    // ─── Solicitar devolución (cliente) ──────────────────────────────────────

    @PostMapping("/mis-pedidos/{id}/devolucion")
    @PreAuthorize("hasAnyRole('CLIENT', 'ADMIN')")
    public String solicitarDevolucion(@PathVariable Long id, Authentication auth, RedirectAttributes flash) {
        try {
            Usuario usuario = usuarioRepo.findByEmail(auth.getName())
                    .orElseThrow(() -> new MagicalBusinessException("Usuario no encontrado"));
            pedidoService.solicitarDevolucion(id, usuario.getCliente());
            flash.addFlashAttribute("success", "Solicitud de devolución del pedido #" + id + " registrada");
        } catch (MagicalBusinessException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/mis-pedidos/" + id;
    }

    // ─── Gestión admin ────────────────────────────────────────────────────────

    @GetMapping("/admin/pedidos")
    @PreAuthorize("hasRole('ADMIN')")
    public String gestionPedidos(
            @RequestParam(required = false, defaultValue = "TODOS") String filtroEstado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            Model model) {

        List<Pedido> pedidos = pedidoService.listarTodos(filtroEstado, fechaDesde, fechaHasta);
        model.addAttribute("pedidos", pedidos);
        model.addAttribute("filtroActual", filtroEstado);
        model.addAttribute("estados", EstadoPedido.values());
        model.addAttribute("fechaDesde", fechaDesde);
        model.addAttribute("fechaHasta", fechaHasta);
        return "admin/pedidos/pedidos-list";
    }

    @PostMapping("/admin/pedidos/estado")
    @PreAuthorize("hasRole('ADMIN')")
    public String actualizarEstado(
            @RequestParam Long pedidoId,
            @RequestParam String nuevoEstado,
            RedirectAttributes flash) {

        try {
            pedidoService.actualizarEstado(pedidoId, EstadoPedido.valueOf(nuevoEstado));
            flash.addFlashAttribute("success", "Estado del pedido #" + pedidoId + " actualizado");
        } catch (Exception e) {
            flash.addFlashAttribute("error", "Error al actualizar el estado: " + e.getMessage());
        }
        return "redirect:/admin/pedidos";
    }
}