package com.magicalAliance.repository.producto;

import com.magicalAliance.entity.producto.Producto;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long> {

    // Reemplaza tu SQL dinámico de 'listar'
    @Query("SELECT p FROM Producto p WHERE " +
            "(:busqueda IS NULL OR p.nombre LIKE %:busqueda%) AND " +
            "(:catId IS NULL OR p.subcategoria.categoria.id = :catId) AND " +
            "(:subId IS NULL OR p.subcategoria.id = :subId)")
    List<Producto> buscarConFiltros(@Param("busqueda") String busqueda,
                                    @Param("catId") Long catId,
                                    @Param("subId") Long subId,
                                    Sort sort);

    @Query("SELECT p FROM Producto p JOIN p.subcategoria s WHERE s.id = :subcatId AND p.id <> :id ORDER BY p.nombre ASC")
    List<Producto> findRelacionadosPorSubcategoria(@Param("subcatId") Long subcatId, @Param("id") Long id);

    @Query("SELECT p FROM Producto p JOIN p.subcategoria s JOIN s.categoria c WHERE c.id = :catId AND p.id <> :id ORDER BY p.nombre ASC")
    List<Producto> findRelacionadosPorCategoria(@Param("catId") Long catId, @Param("id") Long id);

    @Query("SELECT p FROM Producto p WHERE p.id <> :id ORDER BY p.id DESC")
    List<Producto> findRelacionadosRecientes(@Param("id") Long id);
}