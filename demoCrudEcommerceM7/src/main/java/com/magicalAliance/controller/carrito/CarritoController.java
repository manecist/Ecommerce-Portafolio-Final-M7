package com.magicalAliance.controller.carrito;

import com.magicalAliance.dto.carrito.CheckoutDTO;
import com.magicalAliance.entity.carrito.Carrito;
import com.magicalAliance.entity.pedido.Pedido;
import com.magicalAliance.entity.usuario.Cliente;
import com.magicalAliance.entity.usuario.DireccionCliente;
import com.magicalAliance.entity.usuario.Usuario;
import com.magicalAliance.exception.MagicalBusinessException;
import com.magicalAliance.repository.usuario.UsuarioRepository;
import com.magicalAliance.service.carrito.ICarritoService;
import com.magicalAliance.service.pedido.IPedidoService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/carrito")
public class CarritoController {

    @Autowired private ICarritoService carritoService;
    @Autowired private IPedidoService pedidoService;
    @Autowired private UsuarioRepository usuarioRepo;

    // ─── Ver carrito ─────────────────────────────────────────────────────────

    @GetMapping
    public String verCarrito(Authentication auth, HttpSession session, Model model) {
        Carrito carrito = carritoService.obtenerOCrearCarrito(auth, session);
        model.addAttribute("carrito", carrito);
        return "client/carrito/carrito";
    }

    // ─── Agregar producto (AJAX) ──────────────────────────────────────────────

    @PostMapping("/agregar")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> agregar(
            @RequestParam Long productoId,
            @RequestParam(defaultValue = "1") int cantidad,
            Authentication auth,
            HttpSession session) {

        try {
            Carrito carrito = carritoService.obtenerOCrearCarrito(auth, session);
            carritoService.agregarProducto(carrito, productoId, cantidad);

            // Recargar para tener el total actualizado
            Carrito actualizado = carritoService.obtenerOCrearCarrito(auth, session);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "totalItems", actualizado.getTotalItems(),
                    "mensaje", "Producto agregado al carrito"
            ));
        } catch (MagicalBusinessException e) {
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "mensaje", e.getMessage()
            ));
        }
    }

    // ─── Actualizar cantidad (AJAX) ───────────────────────────────────────────

    @PostMapping("/actualizar")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> actualizar(
            @RequestParam Long itemId,
            @RequestParam int cantidad,
            Authentication auth,
            HttpSession session) {

        try {
            carritoService.actualizarCantidad(itemId, cantidad);
            Carrito carrito = carritoService.obtenerOCrearCarrito(auth, session);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "totalItems", carrito.getTotalItems(),
                    "total", carrito.getTotal()
            ));
        } catch (MagicalBusinessException e) {
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "mensaje", e.getMessage()
            ));
        }
    }

    // ─── Eliminar ítem ────────────────────────────────────────────────────────

    @PostMapping("/eliminar/{itemId}")
    public String eliminarItem(@PathVariable Long itemId, RedirectAttributes flash) {
        try {
            carritoService.eliminarItem(itemId);
            flash.addFlashAttribute("success", "Ítem eliminado del carrito");
        } catch (MagicalBusinessException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/carrito";
    }

    // ─── Aplicar cupón ────────────────────────────────────────────────────────

    @PostMapping("/aplicar-cupon")
    public String aplicarCupon(
            @RequestParam String codigo,
            Authentication auth,
            HttpSession session,
            RedirectAttributes flash) {
        try {
            Carrito carrito = carritoService.obtenerOCrearCarrito(auth, session);
            carritoService.aplicarCupon(carrito, codigo);
            flash.addFlashAttribute("success", "Cupón aplicado correctamente");
        } catch (MagicalBusinessException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/carrito";
    }

    // ─── Quitar cupón ─────────────────────────────────────────────────────────

    @PostMapping("/quitar-cupon")
    public String quitarCupon(Authentication auth, HttpSession session, RedirectAttributes flash) {
        Carrito carrito = carritoService.obtenerOCrearCarrito(auth, session);
        carritoService.quitarCupon(carrito);
        flash.addFlashAttribute("success", "Cupón eliminado del carrito");
        return "redirect:/carrito";
    }

    // ─── Vaciar carrito ───────────────────────────────────────────────────────

    @PostMapping("/vaciar")
    public String vaciarCarrito(Authentication auth, HttpSession session, RedirectAttributes flash) {
        Carrito carrito = carritoService.obtenerOCrearCarrito(auth, session);
        carritoService.vaciarCarrito(carrito);
        flash.addFlashAttribute("success", "El carrito fue vaciado");
        return "redirect:/carrito";
    }

    // ─── Vista checkout ───────────────────────────────────────────────────────

    @GetMapping("/checkout")
    public String verCheckout(Authentication auth, HttpSession session, Model model) {
        Carrito carrito = carritoService.obtenerOCrearCarrito(auth, session);

        if (carrito.getItems().isEmpty()) {
            return "redirect:/carrito";
        }

        CheckoutDTO dto = new CheckoutDTO();
        dto.setCarritoId(carrito.getId());

        // Pre-rellenar datos si el usuario está autenticado
        if (estaAutenticado(auth)) {
            obtenerUsuario(auth).ifPresent(u -> {
                Cliente c = u.getCliente();
                if (c != null) {
                    dto.setNombreContacto(c.getNombre() + " " + c.getApellido());
                    dto.setEmailContacto(u.getEmail());
                    dto.setTelefonoContacto(c.getTelefono());

                    // Pre-rellenar campos separados de la dirección principal
                    c.getDirecciones().stream()
                            .filter(DireccionCliente::isEsPrincipal)
                            .findFirst()
                            .ifPresent(dir -> {
                                dto.setCalle(dir.getDireccion());
                                dto.setCiudad(dir.getCiudad());
                                dto.setEstadoRegion(dir.getEstadoRegion());
                                dto.setPais(dir.getPais());
                                dto.setCodigoPostal(dir.getCodigoPostal());
                            });
                }
                model.addAttribute("direcciones", u.getCliente() != null
                        ? u.getCliente().getDirecciones() : null);
            });
        }

        model.addAttribute("carrito", carrito);
        model.addAttribute("checkoutDTO", dto);
        return "client/carrito/checkout";
    }

    // ─── Procesar checkout ────────────────────────────────────────────────────

    @PostMapping("/checkout")
    public String procesarCheckout(
            @Valid @ModelAttribute("checkoutDTO") CheckoutDTO dto,
            BindingResult bindingResult,
            Authentication auth,
            HttpSession session,
            Model model,
            RedirectAttributes flash) {

        Carrito carrito = carritoService.obtenerOCrearCarrito(auth, session);

        if (bindingResult.hasErrors()) {
            model.addAttribute("carrito", carrito);
            if (estaAutenticado(auth)) {
                obtenerUsuario(auth).ifPresent(u ->
                        model.addAttribute("direcciones",
                                u.getCliente() != null ? u.getCliente().getDirecciones() : null));
            }
            return "client/carrito/checkout";
        }

        try {
            // Obtener cliente si está autenticado (null para invitados)
            Cliente cliente = null;
            if (estaAutenticado(auth)) {
                cliente = obtenerUsuario(auth)
                        .map(Usuario::getCliente)
                        .orElse(null);
            }

            Pedido pedido = pedidoService.crearDesdeCarrito(carrito, dto, cliente);
            flash.addFlashAttribute("pedidoId", pedido.getId());
            return "redirect:/carrito/confirmacion/" + pedido.getId();

        } catch (MagicalBusinessException e) {
            model.addAttribute("carrito", carrito);
            model.addAttribute("error", e.getMessage());
            if (estaAutenticado(auth)) {
                obtenerUsuario(auth).ifPresent(u ->
                        model.addAttribute("direcciones",
                                u.getCliente() != null ? u.getCliente().getDirecciones() : null));
            }
            return "client/carrito/checkout";
        }
    }

    // ─── Confirmación de pedido ───────────────────────────────────────────────

    @GetMapping("/confirmacion/{pedidoId}")
    public String confirmacion(@PathVariable Long pedidoId, Model model) {
        Pedido pedido = pedidoService.buscarPorId(pedidoId)
                .orElseThrow(() -> new MagicalBusinessException("Pedido #" + pedidoId + " no encontrado"));
        model.addAttribute("pedido", pedido);
        return "client/carrito/confirmacion";
    }

    // ─── Helpers privados ─────────────────────────────────────────────────────

    private boolean estaAutenticado(Authentication auth) {
        return auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken);
    }

    private Optional<Usuario> obtenerUsuario(Authentication auth) {
        return usuarioRepo.findByEmail(auth.getName());
    }
}