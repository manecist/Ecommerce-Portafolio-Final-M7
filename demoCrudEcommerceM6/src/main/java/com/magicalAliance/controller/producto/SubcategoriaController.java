package com.magicalAliance.controller.producto;

import com.magicalAliance.entity.producto.Subcategoria;
import com.magicalAliance.exception.MagicalBusinessException;
import com.magicalAliance.exception.MagicalNotFoundException;
import com.magicalAliance.service.producto.ICategoriaService;
import com.magicalAliance.service.producto.ISubcategoriaService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


@Controller
@RequestMapping("/admin/subcategorias") // Ruta actualizada para el reino de administración
@PreAuthorize("hasRole('ADMIN')")
public class SubcategoriaController {

    @Autowired
    private ISubcategoriaService subService;

    @Autowired
    private ICategoriaService catService;

    /**
     * LISTAR Y MOSTRAR FORMULARIOS
     * Reemplaza el antiguo switch(action) de los servlets
     */
    @Transactional(readOnly = true)
    @GetMapping({"", "/", "/gestion"})
    public String listar(Model model,
                         @RequestParam(name = "action", required = false) String action,
                         @RequestParam(name = "id", required = false) Long id) {

        try {
            model.addAttribute("subcategorias", subService.listarTodas());
            model.addAttribute("categorias", catService.listarTodas());

            // Lógica de edición
            if ("edit".equals(action) && id != null) {
                Subcategoria sub = subService.buscarPorId(id)
                        .orElseThrow(() -> new MagicalNotFoundException("La subcategoría no existe en el reino."));
                model.addAttribute("subcategoriaEnEdicion", sub);
            } else {
                if (!model.containsAttribute("subcategoriaEnEdicion")) {
                    model.addAttribute("subcategoriaEnEdicion", new Subcategoria());
                }
            }
        } catch (MagicalNotFoundException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("subcategoriaEnEdicion", new Subcategoria());
        }

        return "admin/inventario/subcategorias-list"; // Ruta física corregida
    }

    /**
     * GUARDAR O ACTUALIZAR
     */
    @Transactional
    @PostMapping("/guardar")
    public String guardar(@Valid @ModelAttribute("subcategoriaEnEdicion") Subcategoria subcategoria,
                          BindingResult result,
                          RedirectAttributes flash,
                          Model model) {

        // Si hay errores de validación, recargamos la lista para Mane
        if (result.hasErrors()) {
            model.addAttribute("subcategorias", subService.listarTodas());
            model.addAttribute("categorias", catService.listarTodas());
            return "admin/inventario/subcategorias-list";
        }

        try {
            // Si el ID es 0 o viene vacío, nos aseguramos que sea null para INSERT
            if (subcategoria.getId() != null && subcategoria.getId() == 0) {
                subcategoria.setId(null);
            }

            subService.guardar(subcategoria);
            flash.addFlashAttribute("success", "La esencia de la subcategoría ha sido guardada.");

        } catch (MagicalBusinessException e) {
            // Captura nombres duplicados o validaciones de negocio
            flash.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            flash.addFlashAttribute("error", "Error místico: No se pudo procesar la solicitud.");
        }

        return "redirect:/admin/subcategorias/gestion";
    }

    /**
     * ELIMINAR
     */
    @Transactional
    @PostMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Long id, RedirectAttributes flash) {
        try {
            subService.eliminar(id);
            flash.addFlashAttribute("success", "Subcategoría desvanecida correctamente.");
        } catch (MagicalBusinessException e) {
            // Captura el error de integridad (si tiene productos asociados)
            flash.addFlashAttribute("error", "No puedes eliminarla: " + e.getMessage());
        } catch (Exception e) {
            flash.addFlashAttribute("error", "Error crítico al intentar eliminar.");
        }
        return "redirect:/admin/subcategorias/gestion";
    }
}