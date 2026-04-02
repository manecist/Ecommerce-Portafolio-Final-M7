package com.magicalAliance.repository.descuento;

import com.magicalAliance.entity.descuento.Descuento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface DescuentoRepository extends JpaRepository<Descuento, Long> {

    @Query("SELECT d FROM Descuento d WHERE d.alcance = 'PRODUCTO' AND d.producto.id = :productoId " +
           "AND d.activo = true AND d.fechaInicio <= :hoy AND d.fechaFin >= :hoy")
    List<Descuento> findActivosParaProducto(@Param("productoId") Long productoId, @Param("hoy") LocalDate hoy);

    @Query("SELECT d FROM Descuento d WHERE d.alcance = 'SUBCATEGORIA' AND d.subcategoria.id = :subcatId " +
           "AND d.activo = true AND d.fechaInicio <= :hoy AND d.fechaFin >= :hoy")
    List<Descuento> findActivosParaSubcategoria(@Param("subcatId") Long subcatId, @Param("hoy") LocalDate hoy);

    @Query("SELECT d FROM Descuento d WHERE d.alcance = 'CATEGORIA' AND d.categoria.id = :catId " +
           "AND d.activo = true AND d.fechaInicio <= :hoy AND d.fechaFin >= :hoy")
    List<Descuento> findActivosParaCategoria(@Param("catId") Long catId, @Param("hoy") LocalDate hoy);

    @Query("SELECT d FROM Descuento d WHERE d.alcance = 'GLOBAL' " +
           "AND d.activo = true AND d.fechaInicio <= :hoy AND d.fechaFin >= :hoy")
    List<Descuento> findActivosGlobales(@Param("hoy") LocalDate hoy);

    List<Descuento> findAllByOrderByFechaInicioDesc();
}
