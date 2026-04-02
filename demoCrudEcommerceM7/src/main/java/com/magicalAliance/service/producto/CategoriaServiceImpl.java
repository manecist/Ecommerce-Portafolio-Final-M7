package com.magicalAliance.service.producto;

import com.magicalAliance.entity.producto.Categoria;
import com.magicalAliance.exception.MagicalBusinessException;
import com.magicalAliance.exception.MagicalNotFoundException;
import com.magicalAliance.repository.producto.CategoriaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;


@Service
public class CategoriaServiceImpl implements ICategoriaService {

    @Autowired
    private CategoriaRepository categoriaRepo;

    @Override
    @Transactional(readOnly = true)
    public List<Categoria> listarTodas() {
        return categoriaRepo.findAllByOrderByNombreAsc();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Categoria> buscarPorId(Long id) {
        return categoriaRepo.findById(id);
    }

    @Override
    @Transactional
    public void guardar(Categoria cat) {
        if (cat.getNombre() == null || cat.getNombre().isBlank()) {
            throw new MagicalBusinessException("El nombre de la categoría es obligatorio.");
        }

        String nombreTrim = cat.getNombre().trim();
        cat.setNombre(nombreTrim);

        if (cat.getId() == null || cat.getId() == 0) { // Agregamos chequeo de ID 0 por si Thymeleaf envía 0
            if (categoriaRepo.existsByNombre(nombreTrim)) {
                throw new MagicalBusinessException("Ya existe una categoría con el nombre: " + nombreTrim);
            }
        } else {
            // Buscamos la que ya está en la DB
            Categoria existente = categoriaRepo.findById(cat.getId())
                    .orElseThrow(() -> new MagicalNotFoundException("La categoría con ID " + cat.getId() + " no existe."));

            // Validación de duplicados al cambiar nombre
            if (!existente.getNombre().equalsIgnoreCase(nombreTrim)) {
                if (categoriaRepo.existsByNombre(nombreTrim)) {
                    throw new MagicalBusinessException("Ese nombre ya está en uso.");
                }
            }

            // IMPORTANTE: Asegúrate de que el objeto 'cat' mantenga la imagen si no se cambió
            if (cat.getImagenBanner() == null || cat.getImagenBanner().isEmpty()) {
                cat.setImagenBanner(existente.getImagenBanner());
            }
        }

        categoriaRepo.save(cat);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        // Verificamos si existe antes de intentar borrar
        if (!categoriaRepo.existsById(id)) {
            throw new MagicalNotFoundException("No se puede eliminar: La categoría no existe en los registros actuales.");
        }

        try {
            categoriaRepo.deleteById(id);
        } catch (Exception e) {
            // Captura el error de integridad referencial
            // AJUSTE: Usamos BusinessException porque es un impedimento lógico por asociaciones
            throw new MagicalBusinessException("No se puede desvanecer la categoría porque tiene subcategorías o productos asociados.");
        }
    }
}