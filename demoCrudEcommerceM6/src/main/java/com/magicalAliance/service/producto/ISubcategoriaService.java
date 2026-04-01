package com.magicalAliance.service.producto;

import com.magicalAliance.entity.producto.Subcategoria;

import java.util.List;
import java.util.Optional;

public interface ISubcategoriaService {
    List<Subcategoria> listarTodas();
    List<Subcategoria> listarPorCategoria(Long idCat);
    Optional<Subcategoria> buscarPorId(Long id);
    void guardar(Subcategoria sub);
    void eliminar(Long id);
}