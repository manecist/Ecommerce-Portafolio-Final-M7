package com.magicalAliance.service.usuario;


import com.magicalAliance.dto.usuario.RegistroDTO;
import com.magicalAliance.entity.usuario.Cliente;
import com.magicalAliance.entity.usuario.DireccionCliente;
import com.magicalAliance.entity.usuario.Usuario;

import java.util.List;
import java.util.Optional;

public interface IUsuarioService {
    // --- LEER ---
    List<Usuario> listarTodos(String orden);
    Optional<Usuario> buscarPorId(Long id);
    Optional<Usuario> buscarPorEmail(String email);
    Optional<Usuario> buscarPorRut(String rut);
    Optional<Usuario> obtenerPerfilCompleto(String email);
    List<Usuario> buscarPorCriterio(String termino);

    // --- CREAR ---
    Usuario registrarUsuario(Usuario usuario);
    Usuario crearUsuarioDesdeAdmin(Usuario u, Long idRol);

    // --- ACTUALIZAR ---
    Usuario actualizarUsuarioDesdeAdmin(RegistroDTO dto, Long idRol);
    void actualizarCredenciales(Long id, String email, String password);
    Usuario actualizarDatosPersonales(Long id, Cliente datosNuevos, boolean isAdmin);
    void cambiarRol(Long idUsuario, Long idNuevoRol);
    Cliente actualizarOCrearClienteInvitado(Cliente datosEntrantes);


    // --- RECUPERACIÓN DE CONTRASEÑA ---
    void iniciarRecuperacionContrasena(String email);
    void completarRecuperacionContrasena(String token, String nuevaContrasena);

    // --- ELIMINAR ---
    void eliminarUsuario(Long id);

    // --- DIRECCIONES ---
    void agregarDireccion(Long usuarioId, DireccionCliente direccion);
    void establecerDireccionPrincipal(Long usuarioId, Long direccionId);
    void editarDireccion(Long direccionId, DireccionCliente datosNuevos);
    void eliminarDireccion(Long usuarioId, Long direccionId);
}