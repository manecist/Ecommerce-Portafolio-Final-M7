package com.magicalAliance.service.producto;

import com.magicalAliance.entity.producto.Producto;

import java.util.List;
import java.util.Optional;

public interface IProductoService {
    List<Producto> listar(String busqueda, Long catId, Long subId, String criterioOrden, boolean esAdmin);
    Optional<Producto> buscarPorId(Long id);
    void guardar(Producto p);
    void eliminar(Long id);
}