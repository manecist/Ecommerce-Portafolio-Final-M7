package com.magicalAliance.controller.producto;

import com.magicalAliance.entity.producto.Producto;
import com.magicalAliance.exception.MagicalBusinessException;
import com.magicalAliance.exception.MagicalNotFoundException;
import com.magicalAliance.repository.producto.ProductoRepository;
import com.magicalAliance.service.img.IUploadFileService;
import com.magicalAliance.service.producto.ICategoriaService;
import com.magicalAliance.service.producto.IProductoService;
import com.magicalAliance.service.producto.ISubcategoriaService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.magicalAliance.util.PaginacionHelper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;


@Controller
@RequestMapping("/productos")
public class ProductoController {

    @Autowired
    private IProductoService productoService;

    @Autowired
    private ISubcategoriaService subService;

    @Autowired
    private ICategoriaService catService;

    @Autowired
    private IUploadFileService uploadService;

    @Autowired
    private ProductoRepository productoRepository;

    /**
     * CATÁLOGO Y FILTROS (Acceso público/cliente)
     */
    @GetMapping({"", "/"})
    public String listarYFiltrar(
            @RequestParam(required = false) String txtBuscar,
            @RequestParam(required = false) Long idCategoria,
            @RequestParam(required = false) Long idSubcategoria,
            @RequestParam(required = false) String orden,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            Model model, Authentication auth) {

        try {
            boolean esAdmin = (auth != null) && auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

            List<Producto> todos = productoService.listar(txtBuscar, idCategoria, idSubcategoria, orden, esAdmin);
            model.addAttribute("productos", PaginacionHelper.paginar(todos, page, size, model));
            model.addAttribute("listaCategorias", catService.listarTodas());

            // Si hay categoría seleccionada, cargamos sus subcategorías para los botones de filtro
            if (idCategoria != null && idCategoria > 0) {
                model.addAttribute("listaSubcategorias", subService.listarPorCategoria(idCategoria));
            }

            model.addAttribute("idCatActual", idCategoria);
            model.addAttribute("idSubCatActual", idSubcategoria);

        } catch (Exception e) {
            model.addAttribute("error", "Hubo un problema al canalizar el catálogo de productos.");
        }

        return "public/productos"; // Ruta física de tu carpeta public
    }

    /**
     * DETALLE DE PRODUCTO (Acceso público)
     */
    @GetMapping("/{id:\\d+}")
    public String verDetalle(@PathVariable Long id,
                             @RequestParam(required = false) Long idCategoria,
                             @RequestParam(required = false) Long idSubcategoria,
                             @RequestParam(required = false) String orden,
                             @RequestParam(required = false) String txtBuscar,
                             Model model, Authentication auth) {
        try {
            Producto producto = productoService.buscarPorId(id)
                    .orElseThrow(() -> new MagicalNotFoundException("El producto no existe en el inventario."));

            boolean esAdmin = (auth != null) && auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

            // Relacionados: acumulativo — subcategoría primero, luego categoría, luego recientes
            Set<Long> vistos = new LinkedHashSet<>();
            List<Producto> relacionados = new ArrayList<>();

            for (Producto r : productoRepository.findRelacionadosPorSubcategoria(
                    producto.getSubcategoria().getId(), id)) {
                if (esAdmin || r.getStock() > 0) {
                    relacionados.add(r);
                    vistos.add(r.getId());
                }
            }

            if (relacionados.size() < 8) {
                for (Producto r : productoRepository.findRelacionadosPorCategoria(
                        producto.getSubcategoria().getCategoria().getId(), id)) {
                    if (!vistos.contains(r.getId()) && (esAdmin || r.getStock() > 0)) {
                        relacionados.add(r);
                        vistos.add(r.getId());
                        if (relacionados.size() == 8) break;
                    }
                }
            }

            if (relacionados.size() < 4) {
                for (Producto r : productoRepository.findRelacionadosRecientes(id)) {
                    if (!vistos.contains(r.getId()) && (esAdmin || r.getStock() > 0)) {
                        relacionados.add(r);
                        vistos.add(r.getId());
                        if (relacionados.size() == 8) break;
                    }
                }
            }

            if (relacionados.size() > 8) {
                relacionados = new ArrayList<>(relacionados.subList(0, 8));
            }

            model.addAttribute("producto", producto);
            model.addAttribute("relacionados", relacionados);
            model.addAttribute("esAdminDetalle", esAdmin);
            // Parámetros para reconstruir la URL de regreso
            model.addAttribute("returnCatId", idCategoria);
            model.addAttribute("returnSubCatId", idSubcategoria);
            model.addAttribute("returnOrden", orden);
            model.addAttribute("returnBuscar", txtBuscar);

        } catch (MagicalNotFoundException e) {
            return "redirect:/productos";
        }

        return "public/producto-detalle";
    }

    /**
     * FORMULARIO NUEVO / EDITAR (Solo Admin)
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/gestion")
    public String mostrarFormulario(@RequestParam(required = false) String action,
                                    @RequestParam(required = false) Long id, Model model) {

        try {
            model.addAttribute("galeriaImagenes", uploadService.listarGaleria());
            model.addAttribute("subcategorias", subService.listarTodas());

            if ("edit".equals(action) && id != null) {
                Producto p = productoService.buscarPorId(id)
                        .orElseThrow(() -> new MagicalNotFoundException("El producto solicitado no existe en el inventario."));
                model.addAttribute("producto", p);
            } else {
                Producto nuevo = new Producto();
                nuevo.setImagen("default.jpg"); // Imagen base por defecto
                model.addAttribute("producto", nuevo);
            }
        } catch (MagicalNotFoundException e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/productos";
        }

        return "admin/inventario/producto-form";
    }

    /**
     * GUARDAR (Solo Admin)
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/guardar")
    public String guardar(@Valid @ModelAttribute("producto") Producto producto,
                          BindingResult result,
                          @RequestParam(value = "subcategoriaId", required = false) Long subcategoriaId,
                          @RequestParam(value = "archivoImagen", required = false) MultipartFile archivo,
                          @RequestParam(required = false) String imagenExistente,
                          @RequestParam(required = false) String imagenActual,
                          RedirectAttributes flash, Model model) {

        // Resolver la subcategoría explícitamente desde su ID
        if (subcategoriaId != null) {
            subService.buscarPorId(subcategoriaId).ifPresent(producto::setSubcategoria);
        }

        if (result.hasErrors()) {
            model.addAttribute("subcategorias", subService.listarTodas());
            model.addAttribute("galeriaImagenes", uploadService.listarGaleria());
            return "admin/inventario/producto-form";
        }

        try {
            // Si el ID es 0, lo ponemos en null para que JPA haga INSERT
            if (producto.getId() != null && producto.getId() == 0) {
                producto.setId(null);
            }

            // Lógica de imagen: Prioridad 1: Subir nuevo / Prioridad 2: Elegir galería / Prioridad 3: Mantener actual
            String imagenFinal;
            if (archivo != null && !archivo.isEmpty()) {
                // Si había una imagen previa que no es default y no es de galería, la borramos
                if (imagenActual != null && !imagenActual.equals("default.jpg") && !imagenActual.contains("/")) {
                    uploadService.eliminar(imagenActual, "productos");
                }
                imagenFinal = uploadService.copiar(archivo, "productos");
            } else if (imagenExistente != null && !imagenExistente.isEmpty()) {
                imagenFinal = imagenExistente;
            } else {
                imagenFinal = (imagenActual != null && !imagenActual.isEmpty()) ? imagenActual : "default.jpg";
            }

            producto.setImagen(imagenFinal);
            productoService.guardar(producto);

            flash.addFlashAttribute("success", "¡Producto guardado exitosamente en el grimorio!");
            return "redirect:/productos";

        } catch (MagicalBusinessException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("subcategorias", subService.listarTodas());
            model.addAttribute("galeriaImagenes", uploadService.listarGaleria());
            return "admin/inventario/producto-form";
        } catch (IOException e) {
            flash.addFlashAttribute("error", "Error místico al procesar la imagen del objeto.");
            return "redirect:/productos/admin/gestion";
        }
    }

    /**
     * ELIMINAR (Solo Admin)
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/eliminar/{id}")
    public String eliminar(@PathVariable Long id, RedirectAttributes flash) {
        try {
            productoService.eliminar(id);
            flash.addFlashAttribute("success", "Producto desvanecido correctamente.");
        } catch (MagicalBusinessException e) {
            flash.addFlashAttribute("error", "No se puede eliminar: " + e.getMessage());
        } catch (Exception e) {
            flash.addFlashAttribute("error", "Error crítico al intentar eliminar el producto.");
        }
        return "redirect:/productos";
    }
}