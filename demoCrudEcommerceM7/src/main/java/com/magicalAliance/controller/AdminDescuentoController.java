package com.magicalAliance.controller;

import com.magicalAliance.entity.descuento.AlcanceDescuento;
import com.magicalAliance.entity.descuento.Cupon;
import com.magicalAliance.entity.descuento.Descuento;
import com.magicalAliance.entity.descuento.TipoDescuento;
import com.magicalAliance.entity.producto.Categoria;
import com.magicalAliance.entity.producto.Producto;
import com.magicalAliance.entity.producto.Subcategoria;
import com.magicalAliance.exception.MagicalBusinessException;
import com.magicalAliance.repository.producto.CategoriaRepository;
import com.magicalAliance.repository.producto.ProductoRepository;
import com.magicalAliance.repository.producto.SubcategoriaRepository;
import com.magicalAliance.service.descuento.IDescuentoService;
import com.magicalAliance.util.PaginacionHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.List;

@Controller
@PreAuthorize("hasRole('ADMIN')")
public class AdminDescuentoController {

    @Autowired private IDescuentoService descuentoService;
    @Autowired private CategoriaRepository categoriaRepo;
    @Autowired private SubcategoriaRepository subcategoriaRepo;
    @Autowired private ProductoRepository productoRepo;

    // ════════════════════════════════════════════════════════════
    //  DESCUENTOS AUTOMÁTICOS
    // ════════════════════════════════════════════════════════════

    @GetMapping("/admin/descuentos")
    public String listarDescuentos(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {

        Descuento enEdicion = new Descuento();
        if ("edit".equals(action) && id != null) {
            enEdicion = descuentoService.buscarDescuentoPorId(id).orElse(new Descuento());
        }

        List<Descuento> todos = descuentoService.listarDescuentos();
        model.addAttribute("descuentos", PaginacionHelper.paginar(todos, page, size, model));
        model.addAttribute("descuentoEnEdicion", enEdicion);
        model.addAttribute("tiposDescuento", TipoDescuento.values());
        model.addAttribute("alcances", AlcanceDescuento.values());
        model.addAttribute("categorias", categoriaRepo.findAll());
        model.addAttribute("subcategorias", subcategoriaRepo.findAll());
        model.addAttribute("productos", productoRepo.findAll());
        return "admin/descuentos/descuentos-list";
    }

    @PostMapping("/admin/descuentos/guardar")
    public String guardarDescuento(
            @ModelAttribute Descuento descuento,
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) Long subcategoriaId,
            @RequestParam(required = false) Long productoId,
            RedirectAttributes flash) {

        try {
            if (categoriaId != null)    descuento.setCategoria(categoriaRepo.findById(categoriaId).orElse(null));
            if (subcategoriaId != null) descuento.setSubcategoria(subcategoriaRepo.findById(subcategoriaId).orElse(null));
            if (productoId != null)     descuento.setProducto(productoRepo.findById(productoId).orElse(null));

            descuentoService.guardarDescuento(descuento);
            flash.addFlashAttribute("success", "Descuento guardado correctamente");
        } catch (Exception e) {
            flash.addFlashAttribute("error", "Error al guardar el descuento: " + e.getMessage());
        }
        return "redirect:/admin/descuentos";
    }

    @PostMapping("/admin/descuentos/{id}/eliminar")
    public String eliminarDescuento(@PathVariable Long id, RedirectAttributes flash) {
        try {
            descuentoService.eliminarDescuento(id);
            flash.addFlashAttribute("success", "Descuento eliminado correctamente");
        } catch (Exception e) {
            flash.addFlashAttribute("error", "No se pudo eliminar el descuento");
        }
        return "redirect:/admin/descuentos";
    }

    @PostMapping("/admin/descuentos/{id}/toggle")
    public String toggleDescuento(@PathVariable Long id, RedirectAttributes flash) {
        descuentoService.buscarDescuentoPorId(id).ifPresentOrElse(d -> {
            d.setActivo(!d.isActivo());
            descuentoService.guardarDescuento(d);
            flash.addFlashAttribute("success",
                    "Descuento " + (d.isActivo() ? "activado" : "desactivado"));
        }, () -> flash.addFlashAttribute("error", "Descuento no encontrado"));
        return "redirect:/admin/descuentos";
    }

    // ════════════════════════════════════════════════════════════
    //  CUPONES
    // ════════════════════════════════════════════════════════════

    @GetMapping("/admin/cupones")
    public String listarCupones(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {

        Cupon enEdicion = new Cupon();
        if ("edit".equals(action) && id != null) {
            enEdicion = descuentoService.buscarCuponPorId(id).orElse(new Cupon());
        }

        List<Cupon> todos = descuentoService.listarCupones();
        model.addAttribute("cupones", PaginacionHelper.paginar(todos, page, size, model));
        model.addAttribute("cuponEnEdicion", enEdicion);
        model.addAttribute("tiposDescuento", TipoDescuento.values());
        return "admin/descuentos/cupones-list";
    }

    @PostMapping("/admin/cupones/guardar")
    public String guardarCupon(@ModelAttribute Cupon cupon, RedirectAttributes flash) {
        try {
            descuentoService.guardarCupon(cupon);
            flash.addFlashAttribute("success", "Cupón guardado correctamente");
        } catch (Exception e) {
            String msg = e.getMessage() != null && e.getMessage().contains("Duplicate")
                    ? "Ya existe un cupón con ese código"
                    : "Error al guardar el cupón: " + e.getMessage();
            flash.addFlashAttribute("error", msg);
        }
        return "redirect:/admin/cupones";
    }

    @PostMapping("/admin/cupones/{id}/eliminar")
    public String eliminarCupon(@PathVariable Long id, RedirectAttributes flash) {
        try {
            descuentoService.eliminarCupon(id);
            flash.addFlashAttribute("success", "Cupón eliminado correctamente");
        } catch (Exception e) {
            flash.addFlashAttribute("error", "No se pudo eliminar el cupón");
        }
        return "redirect:/admin/cupones";
    }

    @PostMapping("/admin/cupones/{id}/toggle")
    public String toggleCupon(@PathVariable Long id, RedirectAttributes flash) {
        descuentoService.buscarCuponPorId(id).ifPresentOrElse(c -> {
            c.setActivo(!c.isActivo());
            descuentoService.guardarCupon(c);
            flash.addFlashAttribute("success",
                    "Cupón " + (c.isActivo() ? "activado" : "desactivado"));
        }, () -> flash.addFlashAttribute("error", "Cupón no encontrado"));
        return "redirect:/admin/cupones";
    }
}
