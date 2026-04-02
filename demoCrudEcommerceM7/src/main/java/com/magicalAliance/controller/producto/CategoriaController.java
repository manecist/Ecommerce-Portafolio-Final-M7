package com.magicalAliance.controller.producto;

import com.magicalAliance.entity.producto.Categoria;
import com.magicalAliance.exception.MagicalBusinessException;
import com.magicalAliance.exception.MagicalNotFoundException;
import com.magicalAliance.service.img.IUploadFileService;
import com.magicalAliance.service.producto.ICategoriaService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.magicalAliance.util.PaginacionHelper;
import java.io.IOException;
import java.util.Optional;


@Controller
@RequestMapping("/admin/categorias")
@PreAuthorize("hasRole('ADMIN')")
public class CategoriaController {

    @Autowired
    private ICategoriaService categoriaService;

    @Autowired
    private IUploadFileService uploadService;

    @Transactional(readOnly = true)
    @GetMapping({"", "/", "/gestion"})
    public String listar(Model model,
                         @RequestParam(name = "action", required = false) String action,
                         @RequestParam(name = "id", required = false) Long id,
                         @RequestParam(defaultValue = "0") int page,
                         @RequestParam(defaultValue = "10") int size) {
        try {
            model.addAttribute("categorias",
                    PaginacionHelper.paginar(categoriaService.listarTodas(), page, size, model));
            model.addAttribute("galeriaImagenes", uploadService.listarGaleria());

            if ("edit".equals(action) && id != null) {
                Categoria cat = categoriaService.buscarPorId(id)
                        .orElseThrow(() -> new MagicalNotFoundException("Esa categoría no existe en el registro."));
                model.addAttribute("categoriaEnEdicion", cat);
            } else {
                if (!model.containsAttribute("categoriaEnEdicion")) {
                    Categoria nueva = new Categoria();
                    // Empiezo con la imagen por defecto para que Mane no vea un cuadro vacío
                    nueva.setImagenBanner("banner-simple.jpg");
                    model.addAttribute("categoriaEnEdicion", nueva);
                }
            }
        } catch (MagicalNotFoundException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("categoriaEnEdicion", new Categoria());
        }
        // Ruta física del archivo corregida para que cargue tu template
        return "admin/inventario/categorias-list";
    }

    @Transactional
    @PostMapping("/guardar")
    public String guardar(@Valid @ModelAttribute("categoriaEnEdicion") Categoria categoria,
                          BindingResult result,
                          // CAMBIO CLAVE: Renombro 'imagenBanner' a 'archivoImagen' para evitar conflictos con el objeto Categoria
                          @RequestParam(value = "archivoImagen", required = false) MultipartFile file,
                          @RequestParam(name = "imagenExistente", required = false) String imagenExistente,
                          @RequestParam(name = "imagenActual", required = false) String imagenActual,
                          RedirectAttributes flash,
                          Model model) {

        // Si el ID es 0, lo ponemos en null para que Hibernate entienda que es un INSERT y no un UPDATE
        if (categoria.getId() != null && categoria.getId() == 0) {
            categoria.setId(null);
        }

        // Si detecto errores de validación, devuelvo a Mane al formulario sin perder lo que escribió
        if (result.hasErrors()) {
            model.addAttribute("categorias", categoriaService.listarTodas());
            model.addAttribute("galeriaImagenes", uploadService.listarGaleria());
            model.addAttribute("categoriaEnEdicion", categoria);
            return "admin/inventario/categorias-list";
        }

        try {
            String imagenFinal;

            // 1. PRIORIDAD MÁXIMA: Si subo un archivo nuevo desde su computadora
            if (file != null && !file.isEmpty()) {
                // Si ya había una imagen que no es la de sistema ni de galería, la borro para no llenar el disco
                if (imagenActual != null && !imagenActual.isEmpty() &&
                        !imagenActual.equals("banner-simple.jpg") && !imagenActual.contains("/")) {
                    uploadService.eliminar(imagenActual, "categorias");
                }
                imagenFinal = uploadService.copiar(file, "categorias");
            }
            // 2. SEGUNDA PRIORIDAD: Si seleccionó una imagen que ya existe en la galería
            else if (imagenExistente != null && !imagenExistente.trim().isEmpty()) {
                System.out.println(">>> seleccion desde la galería: " + imagenExistente);
                imagenFinal = imagenExistente;
            }
            // 3. ÚLTIMA OPCIÓN: Mantengo la imagen que ya tenía o pongo la de respaldo
            else {
                imagenFinal = (imagenActual != null && !imagenActual.isEmpty()) ? imagenActual : "banner-simple.jpg";
            }

            // Le asigno el nombre final de la imagen a mi objeto antes de persistir
            categoria.setImagenBanner(imagenFinal);

            // Dejo un rastro en consola para que yo pueda verificar qué se va a guardar exactamente
            System.out.println(">>> Guardando Categoría [" + categoria.getNombre() + "] con banner: " + imagenFinal);

            categoriaService.guardar(categoria);
            flash.addFlashAttribute("success", "¡Categoría guardada exitosamente!");

        } catch (MagicalBusinessException e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/categorias/gestion";
        } catch (IOException e) {
            flash.addFlashAttribute("error", "Error al procesar la imagen en el disco.");
            return "redirect:/admin/categorias/gestion";
        }

        return "redirect:/admin/categorias/gestion";
    }

    @Transactional
    @PostMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Long id, RedirectAttributes flash) {
        try {
            Optional<Categoria> cat = categoriaService.buscarPorId(id);
            if (cat.isPresent()) {
                String img = cat.get().getImagenBanner();
                // Solo elimino archivos físicos que Mane subió, no borro los de galería (que tienen /)
                if (img != null && !img.equals("banner-simple.jpg") && !img.contains("/")) {
                    uploadService.eliminar(img, "categorias");
                }
                categoriaService.eliminar(id);
                flash.addFlashAttribute("success", "Categoría eliminada correctamente.");
            }
        } catch (MagicalBusinessException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/categorias/gestion";
    }
}