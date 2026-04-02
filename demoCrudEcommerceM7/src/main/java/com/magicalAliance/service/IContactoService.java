package com.magicalAliance.service;

import com.magicalAliance.entity.Contacto;

import java.util.List;

public interface IContactoService {
    void guardar(Contacto contacto);
    List<Contacto> listarTodos();
    void eliminar(Long id);
}