package com.magicalAliance.service.producto;

import com.magicalAliance.entity.producto.Producto;
import com.magicalAliance.exception.MagicalBusinessException;
import com.magicalAliance.exception.MagicalNotFoundException;
import com.magicalAliance.repository.producto.ProductoRepository;
import com.magicalAliance.service.img.IUploadFileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ProductoServiceImpl implements IProductoService {

    @Autowired
    private ProductoRepository productoRepo;

    @Autowired
    private IUploadFileService uploadService;

    @Override // Ahora sí coincidirá con la Interfaz
    @Transactional(readOnly = true)
    public List<Producto> listar(String busqueda, Long catId, Long subId, String criterioOrden, boolean esAdmin) {

        // 1. Definir el orden (Igual que en tu DAO antiguo)
        Sort sort = switch (criterioOrden != null ? criterioOrden : "") {
            case "pmin" -> Sort.by("precio").ascending();
            case "pmax" -> Sort.by("precio").descending();
            case "az"   -> Sort.by("nombre").ascending();
            case "za"   -> Sort.by("nombre").descending();
            default     -> Sort.by("id").descending();
        };

        // 2. Ejecutar la búsqueda con los filtros dinámicos
        List<Producto> productos = productoRepo.buscarConFiltros(busqueda, catId, subId, sort);

        // 3. Lógica de Negocio: Si no es Admin, filtramos los que no tienen stock
        if (!esAdmin) {
            return productos.stream()
                    .filter(p -> p.getStock() > 0)
                    .toList();
        }

        return productos;
    }

    @Override
    @Transactional
    public void guardar(Producto p) {
        // AJUSTE: Validación de negocio básica
        if (p.getPrecio() != null && p.getPrecio() < 0) {
            throw new MagicalBusinessException("El precio del producto no puede ser una cifra negativa.");
        }

        // Aquí NO subimos la imagen, solo validamos que el Producto
        // ya tenga una ruta de imagen asignada por el Controller.
        if (p.getImagen() == null || p.getImagen().isEmpty()) {
            p.setImagen("productos/default.jpg");
        }

        productoRepo.save(p);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Producto> buscarPorId(Long id) {
        // AJUSTE: Mantenemos el Optional para el Controller, pero podrías usar .orElseThrow aquí si prefieres
        return productoRepo.findById(id);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        Producto producto = productoRepo.findById(id)
                .orElseThrow(() -> new MagicalNotFoundException("No se puede eliminar: el producto con ID " + id + " no existe en el catálogo."));

        // Eliminar imagen física del disco antes de borrar el registro
        String imagen = producto.getImagen();
        if (imagen != null && !imagen.equals("default.jpg") && !imagen.contains("/")) {
            uploadService.eliminar(imagen, "productos");
        }

        try {
            productoRepo.deleteById(id);
        } catch (Exception e) {
            throw new MagicalBusinessException("No se puede eliminar el producto porque tiene registros asociados.");
        }
    }

    // Método extra útil para cuando necesitas el objeto sí o sí o lanzar error
    @Transactional(readOnly = true)
    public Producto obtenerPorId(Long id) {
        return productoRepo.findById(id)
                .orElseThrow(() -> new MagicalNotFoundException("El producto solicitado no ha sido encontrado."));
    }
}