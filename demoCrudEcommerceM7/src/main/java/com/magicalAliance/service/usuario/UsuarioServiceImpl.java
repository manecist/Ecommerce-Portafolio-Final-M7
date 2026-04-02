package com.magicalAliance.service.usuario;

import com.magicalAliance.dto.usuario.RegistroDTO;
import com.magicalAliance.entity.usuario.Cliente;
import com.magicalAliance.entity.usuario.DireccionCliente;
import com.magicalAliance.entity.usuario.PasswordResetToken;
import com.magicalAliance.entity.usuario.Rol;
import com.magicalAliance.entity.usuario.Usuario;
import com.magicalAliance.exception.MagicalBusinessException;
import com.magicalAliance.exception.MagicalNotFoundException;
import com.magicalAliance.mapper.UsuarioMapper;
import com.magicalAliance.repository.pedido.PedidoRepository;
import com.magicalAliance.repository.usuario.ClienteRepository;
import com.magicalAliance.repository.usuario.DireccionRepository;
import com.magicalAliance.repository.usuario.PasswordResetTokenRepository;
import com.magicalAliance.repository.usuario.RolRepository;
import com.magicalAliance.repository.usuario.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class UsuarioServiceImpl implements IUsuarioService {

    @Autowired private UsuarioRepository usuarioRepo;
    @Autowired private RolRepository rolRepo;
    @Autowired private DireccionRepository direccionRepo;
    @Autowired private ClienteRepository clienteRepo;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private UsuarioMapper usuarioMapper;
    @Autowired private com.magicalAliance.service.IEmailService emailService;
    @Autowired private PedidoRepository pedidoRepo;
    @Autowired private PasswordResetTokenRepository tokenRepo;

    // --- BUSQUEDAS ---

    @Override @Transactional(readOnly = true) public Optional<Usuario> buscarPorId(Long id) { return usuarioRepo.findById(id); }
    @Override @Transactional(readOnly = true) public Optional<Usuario> buscarPorEmail(String email) { return usuarioRepo.findByEmail(email); }

    @Override
    @Transactional(readOnly = true)
    public List<Usuario> listarTodos(String orden) {
        Sort sort;

        switch (orden != null ? orden : "id_asc") {
            case "nombre_az":
                sort = Sort.by("cliente.nombre").ascending();
                break;
            case "nombre_za":
                // AJUSTE: Se agregó .descending() para que el filtro Z-A funcione
                sort = Sort.by("cliente.nombre").descending();
                break;
            case "fecha_asc":
                sort = Sort.by("fechaRegistro").ascending();
                break;
            case "fecha_desc":
                // AJUSTE: Se agregó .descending() para que el filtro de fecha reciente funcione
                sort = Sort.by("fechaRegistro").descending();
                break;
            default:
                sort = Sort.by("id").ascending();
                break;
        }

        return usuarioRepo.findAll(sort);
    }

    // --- HERRAMIENTA DE LIMPIEZA (Privada) ---
    private String limpiarRut(String rut) {
        if (rut == null) return null;
        return rut.replace(".", "").replace("-", "").replace(" ", "").toUpperCase();
    }

    // --- BUSQUEDAS ACTUALIZADAS ---
    @Override
    @Transactional(readOnly = true)
    public Optional<Usuario> buscarPorRut(String rut) {
        return usuarioRepo.findByClienteRut(limpiarRut(rut));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Usuario> buscarPorCriterio(String termino) {
        String limpio = limpiarRut(termino);
        // método con LIKE del Repository
        return usuarioRepo.buscarPorCriterioFlexible(limpio);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Usuario> obtenerPerfilCompleto(String email) {
        return usuarioRepo.findByEmail(email).map(u -> {
            if (u.getCliente() != null) u.getCliente().getDirecciones().size();
            return u;
        });
    }

    // --- REGISTRO ---
    @Override
    @Transactional
    public Usuario registrarUsuario(Usuario usuario) {
        // 1. Validación de edad (Usando el método de la entidad Cliente que ya tienes)
        if (usuario.getCliente() != null) {
            if (!usuario.getCliente().esMayorDeEdad()) {
                throw new MagicalBusinessException("El guardiana debe ser mayor de 18 años para unirse a la Alianza.");
            }
            // Validamos el límite superior de 105 años
            if (usuario.getCliente().getEdad() > 105) {
                throw new MagicalBusinessException("La edad máxima permitida es 105 años.");
            }
        }

        // 2. Limpieza de RUT (Fundamental para evitar duplicados como "12.345-6" vs "123456")
        String rutLimpio = limpiarRut(usuario.getCliente().getRut());
        usuario.getCliente().setRut(rutLimpio);

        // 3. Verificación de Duplicados
        if (usuarioRepo.findByEmail(usuario.getEmail()).isPresent()) {
            throw new MagicalBusinessException("El email " + usuario.getEmail() + " ya tiene una cuenta activa.");
        }

        if (usuarioRepo.findByClienteRut(rutLimpio).isPresent()) {
            throw new MagicalBusinessException("Este RUT ya tiene una cuenta de usuario vinculada.");
        }

        // 4. Validación y Encriptación de Contraseña
        if (usuario.getPassword() == null || usuario.getPassword().isBlank()) {
            throw new MagicalBusinessException("La contraseña es obligatoria para registrarse.");
        }
        if (!usuario.getPassword().matches("^(?=.*[A-Z])(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]).{8,}$")) {
            throw new MagicalBusinessException("La contraseña debe tener mínimo 8 caracteres, una mayúscula y un carácter especial.");
        }
        usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));

        // 5. Verificación de Cliente Existente (El flujo de Invitado que mencionaste)
        // Si ya existe en la tabla clientes (por una compra previa), lo rescatamos para no duplicar.
        clienteRepo.findByRut(rutLimpio).ifPresent(existente -> {
            // Fusionamos datos si es necesario o simplemente vinculamos
            usuario.setCliente(existente);
        });

        // 6. Guardado (Gracias al CascadeType.ALL en Usuario, se guarda el Cliente automáticamente)
        Usuario guardado = usuarioRepo.save(usuario);

        // 7. Vincular pedidos realizados como invitado con este mismo email
        if (guardado.getCliente() != null) {
            pedidoRepo.vincularPedidosInvitado(guardado.getCliente(), guardado.getEmail());
        }

        // 8. Enviar correo de bienvenida (fallo silencioso)
        try { emailService.enviarBienvenidaRegistro(guardado); } catch (Exception ignored) {}

        return guardado;
    }

    @Override
    @Transactional
    public Usuario crearUsuarioDesdeAdmin(Usuario u, Long idRol) {
        // AJUSTE: Validación de rango de edad
        if (u.getCliente().getFechaNacimiento() != null) {
            int edad = u.getCliente().getEdad();
            if (edad < 18 || edad > 105) throw new MagicalBusinessException("La edad debe estar entre 18 y 105 años.");
        }

        String rutLimpio = limpiarRut(u.getCliente().getRut());
        u.getCliente().setRut(rutLimpio);

        // --- LÓGICA DE FUSIÓN ---
        Optional<Cliente> clienteExistente = clienteRepo.findByRut(rutLimpio);

        if (clienteExistente.isPresent()) {
            // ¿Ya tiene una cuenta de usuario?
            if (usuarioRepo.findByClienteRut(rutLimpio).isPresent()) {
                throw new MagicalBusinessException("Este RUT ya tiene una cuenta de usuario activa.");
            }
            // Si existe como cliente pero NO tiene usuario: LO FUSIONAMOS
            // Actualizamos los datos del cliente existente con los que mandó el admin ahora
            Cliente c = clienteExistente.get();
            c.setNombre(u.getCliente().getNombre());
            c.setApellido(u.getCliente().getApellido());
            c.setTelefono(u.getCliente().getTelefono());
            c.setFechaNacimiento(u.getCliente().getFechaNacimiento());

            // Reemplazamos el cliente nuevo por el persistente (el de la BD)
            u.setCliente(c);
        }

        // 3. VALIDACIÓN: ¿Ya existe este Email en la tabla Usuarios?
        if (usuarioRepo.findByEmail(u.getEmail()).isPresent()) {
            throw new MagicalBusinessException("Error: El email " + u.getEmail() + " ya está registrado.");
        }

        // 4. Validación y Encriptación de Contraseña
        if (u.getPassword() == null || u.getPassword().isBlank()) {
            throw new MagicalBusinessException("La contraseña es obligatoria al crear un usuario.");
        }
        if (!u.getPassword().matches("^(?=.*[A-Z])(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]).{8,}$")) {
            throw new MagicalBusinessException("La contraseña debe tener mínimo 8 caracteres, una mayúscula y un carácter especial.");
        }
        u.setPassword(passwordEncoder.encode(u.getPassword()));

        // 5. Asignamos el Rol elegido en el formulario
        Rol rol = rolRepo.findById(idRol).orElseThrow(() ->
                new MagicalNotFoundException("Error: El Rol con ID " + idRol + " no existe."));
        u.setRol(rol);

        // 6. Sincronizamos el email del cliente con el del usuario (Información Espejo)
        u.getCliente().setEmail(u.getEmail());

        // 7. Guardamos todo (Gracias al Cascade, se guarda Usuario y Cliente a la vez)
        return usuarioRepo.save(u);
    }

    // --- CAMBIAR ROL CON SEGURIDAD ---
    @Transactional
    public void cambiarRol(Long idUsuario, Long idNuevoRol) {
        // CANDADO 1: No se puede cambiar el rol al ID 1 (Tú)
        if (idUsuario == 1) {
            throw new MagicalBusinessException("No está permitido modificar el rol del Administrador Principal.");
        }

        Usuario u = usuarioRepo.findById(idUsuario)
                .orElseThrow(() -> new MagicalNotFoundException("Usuario no encontrado"));

        Rol nuevoRol = rolRepo.findById(idNuevoRol)
                .orElseThrow(() -> new MagicalNotFoundException("Rol no encontrado"));

        u.setRol(nuevoRol);
        usuarioRepo.save(u);
    }

    // --- EDITAR CREDENCIALES (Email/Pass) ---

    @Override
    @Transactional
    public Usuario actualizarUsuarioDesdeAdmin(RegistroDTO dto, Long idRol) {
        // 1. BUSQUEDA DEL USUARIO EXISTENTE
        Usuario usuarioPersistente = usuarioRepo.findById(dto.getId())
                .orElseThrow(() -> new MagicalNotFoundException("No se encontró el habitante con ID: " + dto.getId()));

        // AGREGADO: Sincronización mediante Mapper para asegurar persistencia de campos como teléfono
        usuarioMapper.updateEntity(dto, usuarioPersistente);

        // 2. VALIDACIÓN Y ACTUALIZACIÓN DE EMAIL (CUENTA Y CLIENTE)
        // Solo validamos si el email cambió respecto al que ya tiene
        if (!usuarioPersistente.getEmail().equalsIgnoreCase(dto.getEmail())) {
            if (usuarioRepo.findByEmail(dto.getEmail()).isPresent()) {
                throw new MagicalBusinessException("El email " + dto.getEmail() + " ya está en uso por otro habitante.");
            }
            usuarioPersistente.setEmail(dto.getEmail());
            // Sincronizamos con la ficha de cliente para que no haya discrepancias
            if (usuarioPersistente.getCliente() != null) {
                usuarioPersistente.getCliente().setEmail(dto.getEmail());
            }
        }

        // 3. ACTUALIZACIÓN DE CONTRASEÑA
        // Solo encriptamos si el Admin escribió una nueva (no viene vacía)
        if (dto.getPassword() != null && !dto.getPassword().isBlank() && !dto.getPassword().equals("********")) {
            usuarioPersistente.setPassword(passwordEncoder.encode(dto.getPassword()));
        }

        // 4. ACTUALIZACIÓN DEL ROL
        Rol nuevoRol = rolRepo.findById(idRol)
                .orElseThrow(() -> new MagicalNotFoundException("El rol solicitado no existe en la Alianza."));
        usuarioPersistente.setRol(nuevoRol);

        // 5. ACTUALIZACIÓN DE DATOS PERSONALES (CLIENTE)
        Cliente c = usuarioPersistente.getCliente();
        c.setNombre(dto.getNombre());
        c.setApellido(dto.getApellido());
        c.setTelefono(dto.getTelefono());
        c.setFechaNacimiento(dto.getFechaNacimiento());

        // 6. VALIDACIÓN DE RUT (LIMPIEZA Y DUPLICADOS)
        String rutLimpio = limpiarRut(dto.getRut());
        // Si el RUT es distinto al actual, verificamos que no lo tenga otro
        if (!c.getRut().equals(rutLimpio)) {
            if (clienteRepo.findByRut(rutLimpio).isPresent()) {
                throw new MagicalBusinessException("El RUT " + dto.getRut() + " ya pertenece a otra guardiana.");
            }
            c.setRut(rutLimpio);
        }

        // 7. VALIDACIÓN DE EDAD (REGLAS DE LA ALIANZA)
        if (c.getEdad() < 18 || c.getEdad() > 105) {
            throw new MagicalBusinessException("La edad debe estar entre 18 y 105 años para ser parte del reino.");
        }

        // 8. GESTIÓN DE DIRECCIÓN COMPLETA (PAÍS, REGIÓN, CIUDAD, CALLE)
        // Buscamos si el cliente ya tiene una dirección principal para actualizarla, sino la creamos.
        if (dto.getDireccion() != null && !dto.getDireccion().isBlank()) {

            // SI EL DTO DICE QUE ES PRINCIPAL, DESMARCAMOS LAS OTRAS
            if (dto.isEsPrincipal()) {
                c.getDirecciones().forEach(d -> d.setEsPrincipal(false));
            }

            DireccionCliente dirPrincipal = c.getDirecciones().stream()
                    .filter(DireccionCliente::isEsPrincipal)
                    .findFirst()
                    .orElse(null);

            if (dirPrincipal == null && !c.getDirecciones().isEmpty()) {
                dirPrincipal = c.getDirecciones().get(0);
            } else if (dirPrincipal == null) {
                // Si no tiene principal (error raro pero posible), creamos una nueva
                dirPrincipal = DireccionCliente.builder()
                        .cliente(c)
                        .esPrincipal(true)
                        .build();
                c.getDirecciones().add(dirPrincipal);
            }

            // Seteamos todos los campos del DTO a la entidad persistente
            dirPrincipal.setPais(dto.getPais());
            dirPrincipal.setEstadoRegion(dto.getEstadoRegion());
            dirPrincipal.setCiudad(dto.getCiudad());
            dirPrincipal.setDireccion(dto.getDireccion());
            dirPrincipal.setCodigoPostal(dto.getCodigoPostal());
            dirPrincipal.setEsPrincipal(dto.isEsPrincipal()); // SINCRONIZACIÓN CON EL CHECKBOX
        }

        // 9. GUARDADO FINAL (CASCADE PERSISTE USUARIO, CLIENTE Y DIRECCIONES)
        return usuarioRepo.save(usuarioPersistente);
    }

    @Override
    @Transactional
    public void actualizarCredenciales(Long id, String email, String password) {
        Usuario u = usuarioRepo.findById(id).orElseThrow(() ->
                new MagicalNotFoundException("Usuario no encontrado"));

        // --- VALIDACIÓN DE DUPLICADOS ---
        if (email != null && !email.isBlank() && !email.equalsIgnoreCase(u.getEmail())) {
            // Buscamos si el nuevo email ya existe en otro ID
            Optional<Usuario> existe = usuarioRepo.findByEmail(email);
            if (existe.isPresent()) {
                throw new MagicalBusinessException("El email " + email + " ya está registrado por otro usuario.");
            }

            u.setEmail(email);
            // Sincronizamos con la ficha de cliente
            if (u.getCliente() != null) {
                u.getCliente().setEmail(email);
            }
        }

        if (password != null && !password.isBlank()) {
            u.setPassword(passwordEncoder.encode(password));
        }

        usuarioRepo.save(u);
    }


    @Transactional
    public Usuario actualizarDatosPersonales(Long id, Cliente nuevos, boolean esAdmin) {
        Usuario u = usuarioRepo.findById(id)
                .orElseThrow(() -> new MagicalNotFoundException("Usuario no encontrado"));
        Cliente c = u.getCliente();

        // 1. Datos básicos (Permitidos para todos)
        if (nuevos.getNombre() != null && !nuevos.getNombre().isBlank())
            c.setNombre(nuevos.getNombre());

        if (nuevos.getApellido() != null && !nuevos.getApellido().isBlank())
            c.setApellido(nuevos.getApellido());

        if (nuevos.getTelefono() != null)
            c.setTelefono(nuevos.getTelefono());

        if (nuevos.getFechaNacimiento() != null)
            c.setFechaNacimiento(nuevos.getFechaNacimiento());

        // 2. Datos sensibles (SOLO SI ES ADMIN)
        if (esAdmin) {
            // Actualizar RUT
            if (nuevos.getRut() != null && !nuevos.getRut().isBlank()) {
                c.setRut(limpiarRut(nuevos.getRut()));
            }
            // Actualizar Email (Si el email está en la entidad Cliente o Usuario)
            if (nuevos.getEmail() != null && !nuevos.getEmail().isBlank()) {
                c.setEmail(nuevos.getEmail());
                // Nota: Si el email es el username en 'Usuario',
                // deberías actualizar u.setEmail(nuevos.getEmail()) también.
            }
        }

        return usuarioRepo.save(u);
    }

    @Override
    @Transactional
    public Cliente actualizarOCrearClienteInvitado(Cliente datosEntrantes) {
        // 1. Limpiamos el RUT antes de cualquier operación
        String rutLimpio = limpiarRut(datosEntrantes.getRut());

        // 2. Buscamos si ya existe en la base de datos
        return clienteRepo.findByRut(rutLimpio).map(clienteExistente -> {

            // 3. Verificamos si este cliente tiene un Usuario asociado
            // Usamos una consulta al repo de usuarios para ver si existe el rut
            boolean esUsuarioRegistrado = usuarioRepo.findByClienteRut(rutLimpio).isPresent();

            if (!esUsuarioRegistrado) {
                // SI ES SOLO INVITADO: Actualizamos todo automáticamente
                clienteExistente.setNombre(datosEntrantes.getNombre());
                clienteExistente.setApellido(datosEntrantes.getApellido());
                clienteExistente.setEmail(datosEntrantes.getEmail());
                clienteExistente.setTelefono(datosEntrantes.getTelefono());
                clienteExistente.setFechaNacimiento(datosEntrantes.getFechaNacimiento());

                // Las direcciones se manejan por el CascadeType.ALL que ya tienes
                if (datosEntrantes.getDirecciones() != null) {
                    clienteExistente.getDirecciones().clear();
                    datosEntrantes.getDirecciones().forEach(d -> {
                        d.setCliente(clienteExistente);
                        clienteExistente.getDirecciones().add(d);
                    });
                }
                return clienteRepo.save(clienteExistente);
            }

            // SI ES USUARIO REGISTRADO: No actualizamos aquí,
            // porque debe hacerlo desde su perfil con su clave.
            return clienteExistente;

        }).orElseGet(() -> {
            // 4. Si el RUT no existe, es un cliente nuevo absoluto
            datosEntrantes.setRut(rutLimpio);
            return clienteRepo.save(datosEntrantes);
        });
    }

    // --- ELIMINAR USUARIO ---
    @Override
    @Transactional
    public void eliminarUsuario(Long id) {
        // CANDADO 2: No se puede eliminar al ID 1
        if (id == 1) {
            throw new MagicalBusinessException("El Administrador Principal no puede ser eliminado.");
        }
        if (!usuarioRepo.existsById(id)) {
            throw new MagicalNotFoundException("El usuario que intentas eliminar no existe.");
        }
        usuarioRepo.deleteById(id);
    }


    // --- GESTIÓN DE DIRECCIONES (CON CAMPOS CORREGIDOS) ---
    @Override
    @Transactional
    public void agregarDireccion(Long usuarioId, DireccionCliente dir) {
        // Busco al usuario para añadirle su nueva morada.
        Usuario u = usuarioRepo.findById(usuarioId).orElseThrow(()->
                new MagicalNotFoundException("Usuario no encontrado"));

        // Si es la primera, la marco como principal por defecto.
        if (u.getCliente().getDirecciones().isEmpty()) {
            dir.setEsPrincipal(true);
        } else if (dir.isEsPrincipal()) {
            // Si el usuario marcó manualmente esta como principal, desmarco las anteriores.
            u.getCliente().getDirecciones().forEach(d -> d.setEsPrincipal(false));
        }

        dir.setCliente(u.getCliente());
        u.getCliente().getDirecciones().add(dir);
        usuarioRepo.save(u);
    }

    @Override
    @Transactional
    public void editarDireccion(Long direccionId, DireccionCliente nuevos) {
        // 1. Buscamos la dirección persistente (la que ya existe en la BD)
        DireccionCliente dirExistente = direccionRepo.findById(direccionId)
                .orElseThrow(() -> new MagicalNotFoundException("Error: La dirección no existe."));

        // 2. Actualizamos respetando tus campos: ciudad, estadoRegion, pais, codigoPostal
        if (nuevos.getDireccion() != null) dirExistente.setDireccion(nuevos.getDireccion());
        if (nuevos.getCiudad() != null) dirExistente.setCiudad(nuevos.getCiudad());
        if (nuevos.getEstadoRegion() != null) dirExistente.setEstadoRegion(nuevos.getEstadoRegion());
        if (nuevos.getPais() != null) dirExistente.setPais(nuevos.getPais());
        if (nuevos.getCodigoPostal() != null) dirExistente.setCodigoPostal(nuevos.getCodigoPostal());

        // Lógica para 'esPrincipal': Si esta se vuelve principal, las demás dejan de serlo.
        if (nuevos.isEsPrincipal()) {
            dirExistente.getCliente().getDirecciones().forEach(d -> d.setEsPrincipal(false));
            dirExistente.setEsPrincipal(true);
        }

        // 3. Guardamos los cambios
        direccionRepo.save(dirExistente);
    }

    // NUEVO MÉTODO PARA EL BOTÓN "Hacer Principal" DEL PERFIL
    @Override
    @Transactional
    public void establecerDireccionPrincipal(Long usuarioId, Long direccionId) {
        // Busco al usuario para manipular su lista de moradas.
        Usuario u = usuarioRepo.findById(usuarioId)
                .orElseThrow(() -> new MagicalNotFoundException("Usuario no encontrado"));

        // Primero quito el rango de principal a todas sus direcciones actuales.
        u.getCliente().getDirecciones().forEach(d -> d.setEsPrincipal(false));

        // Luego busco la dirección específica y le otorgo el rango de principal.
        u.getCliente().getDirecciones().stream()
                .filter(d -> d.getId().equals(direccionId))
                .findFirst()
                .ifPresent(d -> d.setEsPrincipal(true));

        usuarioRepo.save(u);
    }

    // --- RECUPERACIÓN DE CONTRASEÑA ---

    @Override
    @Transactional
    public void iniciarRecuperacionContrasena(String email) {
        // Fallo silencioso: no revelar si el email existe o no
        if (usuarioRepo.findByEmail(email).isEmpty()) return;

        // Eliminar tokens previos del mismo email
        tokenRepo.deleteByEmail(email);

        // Generar token único válido por 1 hora
        String token = UUID.randomUUID().toString();
        tokenRepo.save(PasswordResetToken.builder()
                .token(token)
                .email(email.toLowerCase())
                .fechaExpiracion(LocalDateTime.now().plusHours(1))
                .usado(false)
                .build());

        try { emailService.enviarRecuperacionContrasena(email, token); } catch (Exception ignored) {}
    }

    @Override
    @Transactional
    public void completarRecuperacionContrasena(String token, String nuevaContrasena) {
        PasswordResetToken resetToken = tokenRepo.findByToken(token)
                .orElseThrow(() -> new MagicalBusinessException("El enlace de recuperación no es válido."));

        if (resetToken.isUsado()) {
            throw new MagicalBusinessException("Este enlace ya fue utilizado. Solicita uno nuevo.");
        }
        if (resetToken.isExpirado()) {
            throw new MagicalBusinessException("El enlace ha expirado. Por favor, solicita uno nuevo.");
        }
        if (!nuevaContrasena.matches("^(?=.*[A-Z])(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]).{8,}$")) {
            throw new MagicalBusinessException("La contraseña debe tener mínimo 8 caracteres, una mayúscula y un carácter especial.");
        }

        Usuario usuario = usuarioRepo.findByEmail(resetToken.getEmail())
                .orElseThrow(() -> new MagicalBusinessException("Usuario no encontrado."));

        usuario.setPassword(passwordEncoder.encode(nuevaContrasena));
        usuarioRepo.save(usuario);

        resetToken.setUsado(true);
        tokenRepo.save(resetToken);
    }

    @Override
    @Transactional
    public void eliminarDireccion(Long usuarioId, Long direccionId) {
        Usuario u = usuarioRepo.findById(usuarioId).orElseThrow(() ->
                new MagicalNotFoundException("Error: El habitante no existe."));

        // Verifico si la dirección que voy a borrar es la principal para no dejar al usuario sin una.
        Optional<DireccionCliente> aBorrar = u.getCliente().getDirecciones().stream()
                .filter(d -> d.getId().equals(direccionId)).findFirst();

        boolean eraPrincipal = aBorrar.isPresent() && aBorrar.get().isEsPrincipal();

        // orphanRemoval = true hará que al sacarla de la lista, se borre de la BD
        u.getCliente().getDirecciones().removeIf(d -> d.getId().equals(direccionId));

        // Si borré la principal y aún quedan moradas, elijo la primera de la lista como nueva principal.
        if (eraPrincipal && !u.getCliente().getDirecciones().isEmpty()) {
            u.getCliente().getDirecciones().get(0).setEsPrincipal(true);
        }

        usuarioRepo.save(u);
    }
}