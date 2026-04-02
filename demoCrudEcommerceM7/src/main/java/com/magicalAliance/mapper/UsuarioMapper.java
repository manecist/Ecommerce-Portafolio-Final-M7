package com.magicalAliance.mapper;

import com.magicalAliance.dto.usuario.RegistroDTO;
import com.magicalAliance.entity.usuario.Cliente;
import com.magicalAliance.entity.usuario.DireccionCliente;
import com.magicalAliance.entity.usuario.Rol;
import com.magicalAliance.entity.usuario.Usuario;
import com.magicalAliance.exception.MagicalBusinessException;
import com.magicalAliance.repository.usuario.RolRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class UsuarioMapper {

    @Autowired private RolRepository rolRepo;

    // Para REGISTRO PÚBLICO (AuthController)
    public Usuario toUsuario(RegistroDTO dto) {
        Cliente cliente = Cliente.builder()
                .rut(limpiarRut(dto.getRut()))
                .nombre(dto.getNombre())
                .apellido(dto.getApellido())
                .email(dto.getEmail())
                .telefono(dto.getTelefono())
                .fechaNacimiento(dto.getFechaNacimiento())
                .build();

        DireccionCliente dir = DireccionCliente.builder()
                .direccion(dto.getDireccion())
                .ciudad(dto.getCiudad())
                .estadoRegion(dto.getEstadoRegion())
                .pais(dto.getPais())
                .codigoPostal(dto.getCodigoPostal())
                .esPrincipal(true)
                .cliente(cliente)
                .build();

        cliente.getDirecciones().add(dir);

        return Usuario.builder()
                .email(dto.getEmail())
                .password(dto.getPassword()) // Service encriptará
                .cliente(cliente)
                .rol(rolRepo.findByNombre("ROLE_CLIENT").orElseThrow())
                .build();
    }

    // ← ESTE MÉTODO FALTABA (para UsuarioController Admin)
    public Usuario toEntity(RegistroDTO dto, Long idRol) {
        Rol rol = (idRol != null)
                ? rolRepo.findById(idRol).orElseThrow(() -> new MagicalBusinessException("Rol no encontrado"))
                : rolRepo.findByNombre("ROLE_CLIENT").orElseThrow(() -> new MagicalBusinessException("Rol CLIENT no encontrado"));

        Cliente cliente = Cliente.builder()
                .rut(limpiarRut(dto.getRut()))
                .nombre(dto.getNombre())
                .apellido(dto.getApellido())
                .email(dto.getEmail())
                .telefono(dto.getTelefono())
                .fechaNacimiento(dto.getFechaNacimiento())
                .build();

        DireccionCliente dir = DireccionCliente.builder()
                .direccion(dto.getDireccion())
                .ciudad(dto.getCiudad())
                .estadoRegion(dto.getEstadoRegion())
                .pais(dto.getPais())
                .codigoPostal(dto.getCodigoPostal())
                .esPrincipal(dto.isEsPrincipal())
                .cliente(cliente)
                .build();

        cliente.getDirecciones().add(dir);

        return Usuario.builder()
                .email(dto.getEmail())
                .password(dto.getPassword()) // Service encriptará
                .cliente(cliente)
                .rol(rol)
                .build();
    }

    // Para UPDATE (Admin)
    public void updateEntity(RegistroDTO dto, Usuario usuario) {
        usuario.setEmail(dto.getEmail());

        Cliente c = usuario.getCliente();
        if (c != null) {
            c.setNombre(dto.getNombre());
            c.setApellido(dto.getApellido());
            c.setRut(limpiarRut(dto.getRut()));
            c.setTelefono(dto.getTelefono());
            c.setFechaNacimiento(dto.getFechaNacimiento());
            c.setEmail(dto.getEmail());
        }
    }

    private String limpiarRut(String rut) {
        if (rut == null) return null;
        return rut.replaceAll("[^0-9kK]", "").toUpperCase();
    }
}