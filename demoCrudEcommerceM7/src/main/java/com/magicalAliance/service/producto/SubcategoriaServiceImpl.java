package com.magicalAliance.service.producto;

import com.magicalAliance.entity.producto.Subcategoria;
import com.magicalAliance.exception.MagicalBusinessException;
import com.magicalAliance.exception.MagicalNotFoundException;
import com.magicalAliance.repository.producto.SubcategoriaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class SubcategoriaServiceImpl implements ISubcategoriaService {

    @Autowired
    private SubcategoriaRepository subRepo;

    @Override
    @Transactional(readOnly = true)
    public List<Subcategoria> listarTodas() {
        // Usamos el método que ordena por Categoria > Nombre
        return subRepo.findAllByOrderByCategoriaNombreAscNombreAsc();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Subcategoria> listarPorCategoria(Long idCat) {
        return subRepo.findByCategoriaId(idCat);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Subcategoria> buscarPorId(Long id) {
        return subRepo.findById(id);
    }

    @Override
    @Transactional
    public void guardar(Subcategoria sub) {
        // 1. VALIDACIÓN: Nombre (usando sub.getNombre() de tu entidad)
        if (sub.getNombre() == null || sub.getNombre().isBlank()) {
            throw new MagicalBusinessException("El nombre de la subcategoría es obligatorio para el registro.");
        }

        // 2. VALIDACIÓN: Categoría asociada (para evitar errores de FK en BD)
        if (sub.getCategoria() == null || sub.getCategoria().getId() == null) {
            throw new MagicalBusinessException("Debe seleccionar una categoría base para invocar esta subcategoría.");
        }

        // 3. VALIDACIÓN: Duplicados (Solo si es una subcategoría nueva)
        if (sub.getId() == null) {
            // Verificamos si ya existe ese nombre en la misma categoría
            if (subRepo.existsByNombreAndCategoriaId(sub.getNombre().trim(), sub.getCategoria().getId())) {
                throw new MagicalBusinessException("Ya existe una subcategoría llamada '" + sub.getNombre() + "' en esta categoría.");
            }
        }

        // 4. GUARDAR: JPA hace el Insert o Update automáticamente
        subRepo.save(sub);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        if (!subRepo.existsById(id)) {
            // AJUSTE: Si no existe el ID, lanzamos la de "No Encontrado"
            throw new MagicalNotFoundException("La subcategoría que intenta eliminar no existe en el reino.");
        }
        try {
            subRepo.deleteById(id);
        } catch (Exception e) {
            // Esto pasa si hay productos asociados (integridad referencial)
            // AJUSTE: Error de negocio porque existen dependencias
            throw new MagicalBusinessException("No se puede eliminar: existen productos asociados a esta subcategoría en el inventario.");
        }
    }
}