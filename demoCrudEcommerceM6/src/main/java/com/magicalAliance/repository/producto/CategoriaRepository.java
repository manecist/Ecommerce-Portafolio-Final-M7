package com.magicalAliance.repository.producto;

import com.magicalAliance.entity.producto.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    // Reemplaza a: SELECT ... ORDER BY nombreCategoria ASC
    List<Categoria> findAllByOrderByNombreAsc();

    // Reemplaza a: SELECT COUNT(*) ... WHERE nombreCategoria = ?
    boolean existsByNombre(String nombre);
}