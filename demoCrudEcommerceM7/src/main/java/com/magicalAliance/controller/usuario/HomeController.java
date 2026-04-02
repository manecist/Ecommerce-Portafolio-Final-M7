package com.magicalAliance.controller.usuario;

import com.magicalAliance.entity.usuario.Usuario;
import com.magicalAliance.entity.producto.Categoria;
import com.magicalAliance.entity.producto.Producto;
import com.magicalAliance.exception.MagicalBusinessException;
import com.magicalAliance.exception.MagicalNotFoundException;
import com.magicalAliance.service.carrito.ICarritoService;
import com.magicalAliance.service.producto.ICategoriaService;
import com.magicalAliance.service.producto.IProductoService;
import com.magicalAliance.service.usuario.IUsuarioService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import java.util.Collections;
import java.util.List;


@Controller
@ControllerAdvice
public class HomeController {

    @Autowired
    private IUsuarioService usuarioService;

    @Autowired
    private ICategoriaService catService;

    @Autowired
    private IProductoService prodService;

    @Autowired
    private ICarritoService carritoService;

    @GetMapping({"/home", "/"})
    public String home(Authentication authentication, Model model) {

        try {
            // 1. CARGA DE DATOS PARA EL HOME (Protegido)
            List<Categoria> listaCat = catService.listarTodas();
            model.addAttribute("listaCategorias", listaCat);

            List<Producto> listaProd = prodService.listar(null, null, null, "id_desc", false);
            model.addAttribute("listaProductos", listaProd);

        } catch (MagicalBusinessException | MagicalNotFoundException e) {
            model.addAttribute("error", "Aviso del reino: " + e.getMessage());
        } catch (Exception e) {
            model.addAttribute("error", "Hubo un problema al invocar el catálogo. Intenta refrescar la página.");
        }
        return "public/home";
    }

    @GetMapping("/acceso-denegado")
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String accesoDenegado() {
        return "public/error/403";
    }

    @ModelAttribute
    public void addGlobalAttributes(Model model, Authentication authentication, HttpSession session) {
        // 1. Verificamos si no hay nadie logueado
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {

            model.addAttribute("isLogueado", false);
            model.addAttribute("nombreUsuario", "Invitado");

        } else {
            // 2. Si hay alguien, rescatamos su nombre real
            String email = authentication.getName();

            usuarioService.buscarPorEmail(email).ifPresentOrElse(u -> {
                // Importante: verificar que el Cliente no sea nulo antes de pedir el nombre
                String nombreParaMostrar = (u.getCliente() != null) ? u.getCliente().getNombre() : "Viajero";

                model.addAttribute("nombreUsuario", nombreParaMostrar);
                model.addAttribute("usuarioId", u.getId());
                model.addAttribute("rol", u.getRol().getNombre());
                model.addAttribute("isLogueado", true);
            }, () -> {
                model.addAttribute("nombreUsuario", "Viajero/a");
                model.addAttribute("isLogueado", false);
            });
        }

        // 3. Contador del carrito para el badge del navbar (funciona para usuarios e invitados)
        try {
            int itemsCarrito = carritoService.contarItems(authentication, session);
            model.addAttribute("carritoItemCount", itemsCarrito);
        } catch (Exception e) {
            model.addAttribute("carritoItemCount", 0);
        }

        // 4. Categorías globales para el dropdown del header (todas las páginas)
        try {
            if (!model.containsAttribute("listaCategorias")) {
                model.addAttribute("listaCategorias", catService.listarTodas());
            }
        } catch (Exception e) {
            model.addAttribute("listaCategorias", Collections.emptyList());
        }
    }

}