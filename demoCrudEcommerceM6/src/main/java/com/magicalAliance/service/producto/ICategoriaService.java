package com.magicalAliance.service.producto;

import com.magicalAliance.entity.producto.Categoria;

import java.util.List;
import java.util.Optional;

public interface ICategoriaService {
    List<Categoria> listarTodas();
    Optional<Categoria> buscarPorId(Long id);
    void guardar(Categoria cat);
    void eliminar(Long id);
}