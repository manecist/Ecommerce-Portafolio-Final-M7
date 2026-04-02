package com.magicalAliance.repository.producto;

import com.magicalAliance.entity.producto.Subcategoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubcategoriaRepository extends JpaRepository<Subcategoria, Long> {

    /**
     * LISTAR TODAS
     * 'CategoriaNombre' -> Busca en la entidad Categoria el atributo 'nombre'
     * 'NombreAsc' -> Busca en la entidad Subcategoria el atributo 'nombre'
     */
    List<Subcategoria> findAllByOrderByCategoriaNombreAscNombreAsc();

    /**
     * LISTAR POR CATEGORÍA
     * 'CategoriaId' -> Busca el ID dentro del objeto Categoria
     */
    List<Subcategoria> findByCategoriaId(Long categoriaId);

    /**
     * VALIDAR DUPLICADOS
     * Usamos 'Nombre' porque así se llama tu variable en la clase Subcategoria
     */
    boolean existsByNombreAndCategoriaId(String nombre, Long categoriaId);
}