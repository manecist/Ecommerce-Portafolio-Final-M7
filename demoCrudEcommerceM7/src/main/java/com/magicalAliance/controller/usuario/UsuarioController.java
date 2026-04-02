package com.magicalAliance.controller.usuario;

import com.magicalAliance.dto.usuario.RegistroDTO;
import com.magicalAliance.entity.Suscriptor;
import com.magicalAliance.entity.usuario.Cliente;
import com.magicalAliance.entity.usuario.DireccionCliente;
import com.magicalAliance.entity.usuario.Usuario;
import com.magicalAliance.exception.MagicalBusinessException;
import com.magicalAliance.exception.MagicalNotFoundException;
import com.magicalAliance.mapper.UsuarioMapper;
import com.magicalAliance.repository.SuscriptorRepository;
import com.magicalAliance.repository.usuario.RolRepository;
import com.magicalAliance.service.IEmailService;
import com.magicalAliance.service.usuario.IUsuarioService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.magicalAliance.util.PaginacionHelper;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/usuarios")
public class UsuarioController {

    @Autowired
    private IUsuarioService usuarioService;

    @Autowired
    private RolRepository rolRepo;

    @Autowired
    private UsuarioMapper usuarioMapper;

    @Autowired
    private SuscriptorRepository suscriptorRepository;

    @Autowired
    private IEmailService emailService;

    // --- 1. GESTIÓN ADMIN: Listado, Búsqueda y Ordenamiento ---
    @GetMapping("/admin/gestion")
    @PreAuthorize("hasRole('ADMIN')")
    public String buscarUsuarios(
            @RequestParam(required = false) String criterio,
            @RequestParam(required = false, defaultValue = "id_asc") String orden,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {

        model.addAttribute("roles", rolRepo.findAll());
        model.addAttribute("ordenActual", orden);

        List<Usuario> todos;
        if (criterio != null && !criterio.isBlank()) {
            todos = usuarioService.buscarPorCriterio(criterio);
            model.addAttribute("criterio", criterio);
        } else {
            todos = usuarioService.listarTodos(orden);
        }

        model.addAttribute("usuarios", PaginacionHelper.paginar(todos, page, size, model));
        return "admin/usuarios/usuarios-list";
    }

    // --- 2. REGISTRO POR ADMIN ---
    @GetMapping("/admin/nuevo")
    @PreAuthorize("hasRole('ADMIN')")
    public String formularioNuevoUsuario(Model model) {
        model.addAttribute("nuevoUsuario", new RegistroDTO());
        model.addAttribute("roles", rolRepo.findAll());
        // Inicializamos idRolActual como nulo para el formulario de creación
        model.addAttribute("idRolActual", null);
        return "admin/usuarios/usuario-form";
    }

    // --- MÉTODO DE EDICIÓN PARA ADMIN ---
    @GetMapping("/admin/editar/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String formularioEditarUsuario(@PathVariable Long id, Model model) {
        Usuario u = usuarioService.buscarPorId(id)
                .orElseThrow(() -> new MagicalNotFoundException("El habitante solicitado no existe."));

        RegistroDTO dto = new RegistroDTO();
        dto.setId(u.getId());
        dto.setEmail(u.getEmail());
        dto.setPassword(u.getPassword());

        if (u.getCliente() != null) {
            dto.setNombre(u.getCliente().getNombre());
            dto.setApellido(u.getCliente().getApellido());
            dto.setRut(u.getCliente().getRut());
            dto.setTelefono(u.getCliente().getTelefono());
            dto.setFechaNacimiento(u.getCliente().getFechaNacimiento());

            u.getCliente().getDirecciones().stream()
                    .filter(DireccionCliente::isEsPrincipal)
                    .findFirst()
                    .ifPresent(dir -> {
                        dto.setPais(dir.getPais());
                        dto.setEstadoRegion(dir.getEstadoRegion());
                        dto.setCiudad(dir.getCiudad());
                        dto.setDireccion(dir.getDireccion());
                        dto.setCodigoPostal(dir.getCodigoPostal());
                        dto.setEsPrincipal(true); // Cargamos el estado actual
                    });
        }

        model.addAttribute("nuevoUsuario", dto);
        model.addAttribute("roles", rolRepo.findAll());
        model.addAttribute("idRolActual", u.getRol() != null ? u.getRol().getId() : null);
        // Pasamos el usuario completo para mostrar todas sus direcciones en el formulario
        model.addAttribute("usuarioCompleto", u);

        return "admin/usuarios/usuario-form";
    }

    @PostMapping("/admin/guardar")
    @PreAuthorize("hasRole('ADMIN')")
    public String guardarUsuarioAdmin(@Valid @ModelAttribute("nuevoUsuario") RegistroDTO registroDTO,
                                      BindingResult result,
                                      @RequestParam(required = false) Long idRol,
                                      RedirectAttributes flash,
                                      Model model) {

        if (result.hasErrors()) {
            model.addAttribute("roles", rolRepo.findAll());
            model.addAttribute("idRolActual", idRol);
            if (registroDTO.getId() != null) {
                usuarioService.buscarPorId(registroDTO.getId())
                        .ifPresent(u -> model.addAttribute("usuarioCompleto", u));
            }
            return "admin/usuarios/usuario-form";
        }

        // Forzamos que sea Long y priorizamos el parámetro del Request
        Long rolFinal = (idRol != null) ? idRol : registroDTO.getIdRol();

        if (rolFinal == null) {
            model.addAttribute("error", "Debes asignar un rango (rol) para manifestar a este habitante.");
            model.addAttribute("roles", rolRepo.findAll());
            return "admin/usuarios/usuario-form";
        }

        try {
            if (registroDTO.getId() != null) {
                // Sincroniza con: Usuario actualizarUsuarioDesdeAdmin(RegistroDTO dto, Long idRol)
                this.usuarioService.actualizarUsuarioDesdeAdmin(registroDTO, rolFinal);
                flash.addFlashAttribute("success", "La esencia del habitante ha sido actualizada con éxito.");
            } else {
                // Sincroniza con: Usuario crearUsuarioDesdeAdmin(Usuario u, Long idRol)
                Usuario usuarioParaGuardar = usuarioMapper.toEntity(registroDTO, rolFinal);
                this.usuarioService.crearUsuarioDesdeAdmin(usuarioParaGuardar, rolFinal);
                flash.addFlashAttribute("success", "Habitante '" + registroDTO.getNombre() + "' manifestado con éxito.");
            }

            return "redirect:/usuarios/admin/gestion";

        } catch (MagicalBusinessException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("roles", rolRepo.findAll());
            model.addAttribute("idRolActual", rolFinal);
            if (registroDTO.getId() != null) {
                usuarioService.buscarPorId(registroDTO.getId())
                        .ifPresent(u -> model.addAttribute("usuarioCompleto", u));
            }
            return "admin/usuarios/usuario-form";
        } catch (Exception e) {
            model.addAttribute("error", "Error místico inesperado: " + e.getMessage());
            model.addAttribute("roles", rolRepo.findAll());
            model.addAttribute("idRolActual", rolFinal);
            if (registroDTO.getId() != null) {
                usuarioService.buscarPorId(registroDTO.getId())
                        .ifPresent(u -> model.addAttribute("usuarioCompleto", u));
            }
            return "admin/usuarios/usuario-form";
        }
    }

    // --- 3. CAMBIO DE ROL RÁPIDO ---
    @PostMapping("/admin/cambiar-rol")
    @PreAuthorize("hasRole('ADMIN')")
    public String cambiarRolRapido(@RequestParam Long usuarioId, @RequestParam Long nuevoRolId, RedirectAttributes flash) {
        try {
            if (usuarioId == 1) throw new MagicalBusinessException("Protección: No puedes alterar el rango del Administrador Supremo.");
            usuarioService.cambiarRol(usuarioId, nuevoRolId);
            flash.addFlashAttribute("success", "Rango actualizado correctamente.");
        } catch (MagicalBusinessException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/usuarios/admin/gestion";
    }

    // --- 4. PERFIL: Ver y Navegar ---
    // --- 4. PERFIL: Ver y Navegar ---
    @GetMapping("/perfil/{id}")
    public String verPerfil(@PathVariable Long id, Authentication auth, Model model) {
        Usuario logueado = usuarioService.buscarPorEmail(auth.getName())
                .orElseThrow(() -> new MagicalBusinessException("Sesión expirada."));

        if (!isAdmin(auth) && !logueado.getId().equals(id)) {
            return "redirect:/home?error=no-autorizado";
        }

        Usuario target = usuarioService.buscarPorId(id)
                .flatMap(u -> usuarioService.obtenerPerfilCompleto(u.getEmail()))
                .orElseThrow(() -> new MagicalNotFoundException("El habitante solicitado no existe."));

        model.addAttribute("u", target);
        model.addAttribute("cliente", target.getCliente());
        model.addAttribute("esSuscriptor", suscriptorRepository.existsByEmail(target.getEmail()));

        return "client/perfil/detalle";
    }

    @GetMapping("/perfil")
    public String irMiPerfil(Authentication auth) {
        return usuarioService.buscarPorEmail(auth.getName())
                .map(u -> "redirect:/usuarios/perfil/" + u.getId())
                .orElse("redirect:/login");
    }

    // --- 5. EDICIÓN: Acceso y Datos Personales ---
    @PostMapping("/editar-acceso/{id}")
    public String editarAcceso(@PathVariable Long id, @RequestParam String email,
                               @RequestParam(required = false) String password, Authentication auth, RedirectAttributes flash) {
        if (!isAdmin(auth) && !isDuenio(auth, id)) return "redirect:/home";

        try {
            String emailActual = auth.getName();
            usuarioService.actualizarCredenciales(id, email, password);

            if (!isAdmin(auth) && !emailActual.equalsIgnoreCase(email)) {
                return "redirect:/logout";
            }
            flash.addFlashAttribute("success", "Credenciales actualizadas.");
            return "redirect:/usuarios/perfil/" + id;
        } catch (MagicalBusinessException e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/usuarios/perfil/" + id;
        }

    }


    @PostMapping("/editar-datos/{id}")
    public String guardarCambios(@PathVariable Long id,
                                 @ModelAttribute("cliente") Cliente clienteForm,
                                 Authentication auth,
                                 RedirectAttributes redirect) {

        // Verificamos si es ADMIN para pasarle esa potestad al servicio
        boolean esAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        usuarioService.actualizarDatosPersonales(id, clienteForm, esAdmin);

        redirect.addFlashAttribute("success", "¡Esencia sincronizada con éxito!");
        return "redirect:/usuarios/perfil/" + id;
    }

    // --- 6. GESTIÓN DE DIRECCIONES ---
    @PostMapping("/direcciones/agregar/{usuarioId}")
    public String agregarDir(@PathVariable Long usuarioId, @ModelAttribute DireccionCliente dir, Authentication auth, RedirectAttributes flash) {
        if (!isAdmin(auth) && !isDuenio(auth, usuarioId)) return "redirect:/home";
        try {
            if (dir.getPais() == null || dir.getPais().isBlank()) dir.setPais("Chile");
            usuarioService.agregarDireccion(usuarioId, dir);
            flash.addFlashAttribute("success", "Dirección agregada.");
        } catch (MagicalBusinessException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return redirectDirecciones(usuarioId, auth);
    }

    @PostMapping("/direcciones/agregarAdmin/{usuarioId}")
    public String agregarDirAdmin(@PathVariable Long usuarioId, @ModelAttribute DireccionCliente dir, Authentication auth, RedirectAttributes flash) {
        if (!isAdmin(auth) && !isDuenio(auth, usuarioId)) return "redirect:/home";
        try {
            if (dir.getPais() == null || dir.getPais().isBlank()) dir.setPais("Chile");
            usuarioService.agregarDireccion(usuarioId, dir);
            flash.addFlashAttribute("success", "Dirección agregada.");
        } catch (MagicalBusinessException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return redirectDireccionesAdmin(usuarioId, auth);
    }

    @PostMapping("/direcciones/editar/{usuarioId}/{dirId}")
    public String editarDir(@PathVariable Long usuarioId, @PathVariable Long dirId,
                            @ModelAttribute DireccionCliente nuevos, Authentication auth, RedirectAttributes flash) {
        if (!isAdmin(auth) && !isDuenio(auth, usuarioId)) return "redirect:/home";
        try {
            if (nuevos.getPais() == null || nuevos.getPais().isBlank()) nuevos.setPais("Chile");
            usuarioService.editarDireccion(dirId, nuevos);
            flash.addFlashAttribute("success", "Dirección actualizada.");
        } catch (MagicalBusinessException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return redirectDirecciones(usuarioId, auth);
    }

    @PostMapping("/direcciones/editarAdmin/{usuarioId}/{dirId}")
    public String editarDirAdmin(@PathVariable Long usuarioId, @PathVariable Long dirId,
                            @ModelAttribute DireccionCliente nuevos, Authentication auth, RedirectAttributes flash) {
        if (!isAdmin(auth) && !isDuenio(auth, usuarioId)) return "redirect:/home";
        try {
            if (nuevos.getPais() == null || nuevos.getPais().isBlank()) nuevos.setPais("Chile");
            usuarioService.editarDireccion(dirId, nuevos);
            flash.addFlashAttribute("success", "Dirección actualizada.");
        } catch (MagicalBusinessException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return redirectDireccionesAdmin(usuarioId, auth);
    }

    @PostMapping("/direcciones/principal/{usuarioId}/{dirId}")
    public String establecerPrincipal(@PathVariable Long usuarioId, @PathVariable Long dirId, Authentication auth, RedirectAttributes flash) {
        if (!isAdmin(auth) && !isDuenio(auth, usuarioId)) return "redirect:/home";
        try {
            usuarioService.establecerDireccionPrincipal(usuarioId, dirId);
            flash.addFlashAttribute("success", "Dirección principal actualizada.");
        } catch (MagicalBusinessException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return redirectDirecciones(usuarioId, auth);
    }

    @PostMapping("/direcciones/principalAdmin/{usuarioId}/{dirId}")
    public String establecerPrincipalAdmin(@PathVariable Long usuarioId, @PathVariable Long dirId, Authentication auth, RedirectAttributes flash) {
        if (!isAdmin(auth) && !isDuenio(auth, usuarioId)) return "redirect:/home";
        try {
            usuarioService.establecerDireccionPrincipal(usuarioId, dirId);
            flash.addFlashAttribute("success", "Dirección principal actualizada.");
        } catch (MagicalBusinessException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return redirectDireccionesAdmin(usuarioId, auth);
    }

    @PostMapping("/direcciones/eliminar/{usuarioId}/{dirId}")
    public String eliminarDir(@PathVariable Long usuarioId, @PathVariable Long dirId, Authentication auth, RedirectAttributes flash) {
        if (!isAdmin(auth) && !isDuenio(auth, usuarioId)) return "redirect:/home";
        try {
            usuarioService.eliminarDireccion(usuarioId, dirId);
            flash.addFlashAttribute("success", "Dirección eliminada.");
        } catch (MagicalBusinessException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return redirectDirecciones(usuarioId, auth);
    }

    @PostMapping("/direcciones/eliminarAdmin/{usuarioId}/{dirId}")
    public String eliminarDirAdmin(@PathVariable Long usuarioId, @PathVariable Long dirId, Authentication auth, RedirectAttributes flash) {
        if (!isAdmin(auth) && !isDuenio(auth, usuarioId)) return "redirect:/home";
        try {
            usuarioService.eliminarDireccion(usuarioId, dirId);
            flash.addFlashAttribute("success", "Dirección eliminada.");
        } catch (MagicalBusinessException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return redirectDireccionesAdmin(usuarioId, auth);
    }

    // Redirige al admin al formulario de edición, al cliente a su perfil
    private String redirectDirecciones(Long usuarioId, Authentication auth) {
        return "redirect:/usuarios/perfil/"  + usuarioId;
    }

    // Redirige al admin al formulario de edición, al cliente a su perfil
    private String redirectDireccionesAdmin(Long usuarioId, Authentication auth) {
        if (isAdmin(auth)) {
            return "redirect:/usuarios/admin/gestion";
        }
        return "redirect:/usuarios/perfil/"  + usuarioId;
    }

    // --- 7. ELIMINAR PROPIA CUENTA (usuario autenticado) ---
    @PostMapping("/eliminar-cuenta/{id}")
    public String eliminarCuenta(@PathVariable Long id, Authentication auth,
                                 HttpServletRequest request, RedirectAttributes flash) {
        if (!isDuenio(auth, id)) {
            flash.addFlashAttribute("error", "No puedes eliminar la cuenta de otro habitante.");
            return "redirect:/home";
        }
        try {
            Usuario usuario = usuarioService.buscarPorId(id)
                    .orElseThrow(() -> new MagicalNotFoundException("Usuario no encontrado."));
            emailService.enviarConfirmacionEliminacionCuenta(usuario);
            usuarioService.eliminarUsuario(id);
            SecurityContextHolder.clearContext();
            HttpSession session = request.getSession(false);
            if (session != null) session.invalidate();
            return "redirect:/home";
        } catch (MagicalBusinessException e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/usuarios/perfil/" + id;
        }
    }

    // --- 8. TOGGLE SUSCRIPCIÓN AL BOLETÍN DESDE PERFIL ---
    @PostMapping("/suscripcion/toggle/{id}")
    public String toggleSuscripcion(@PathVariable Long id, Authentication auth, RedirectAttributes flash) {
        if (!isAdmin(auth) && !isDuenio(auth, id)) return "redirect:/home";

        Usuario usuario = usuarioService.buscarPorId(id)
                .orElseThrow(() -> new MagicalNotFoundException("Usuario no encontrado."));
        String email = usuario.getEmail();

        if (suscriptorRepository.existsByEmail(email)) {
            suscriptorRepository.findByEmail(email).ifPresent(suscriptorRepository::delete);
            emailService.enviarConfirmacionBaja(email);
            flash.addFlashAttribute("success", "Te has dado de baja del boletín mágico.");
        } else {
            Suscriptor nuevo = new Suscriptor();
            nuevo.setEmail(email);
            suscriptorRepository.save(nuevo);
            emailService.enviarConfirmacionSuscripcion(email);
            flash.addFlashAttribute("success", "¡Te has suscrito al boletín mágico!");
        }
        return "redirect:/usuarios/perfil/" + id;
    }

    // --- 9. ELIMINAR USUARIO (ADMIN) ---
    @PostMapping("/eliminar/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String eliminar(@PathVariable Long id, Authentication auth, RedirectAttributes flash) {
        try {
            Usuario logueado = usuarioService.buscarPorEmail(auth.getName()).get();
            if (logueado.getId().equals(id)) {
                throw new MagicalBusinessException("No puedes desvanecer tu propia esencia.");
            }
            usuarioService.eliminarUsuario(id);
            flash.addFlashAttribute("success", "Usuario eliminado correctamente.");
        } catch (MagicalBusinessException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/usuarios/admin/gestion";
    }

    @GetMapping("/admin/verificar-rut/{rut}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseBody
    public Optional<Usuario> verificarExistenciaRut(@PathVariable String rut) {
        return usuarioService.buscarPorRut(rut);
    }

    @PostMapping("/invitado/actualizar-datos")
    public String actualizarDatosInvitado(@ModelAttribute Cliente datosInvitado, RedirectAttributes flash) {
        try {
            usuarioService.actualizarOCrearClienteInvitado(datosInvitado);
            flash.addFlashAttribute("success", "Tus datos de contacto han sido actualizados.");
        } catch (Exception e) {
            flash.addFlashAttribute("error", "Hubo un problema al sintonizar tus datos.");
        }
        return "redirect:/home";
    }

    private boolean isAdmin(Authentication auth) {
        return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    private boolean isDuenio(Authentication auth, Long id) {
        return usuarioService.buscarPorEmail(auth.getName())
                .map(u -> u.getId().equals(id))
                .orElse(false);
    }
}