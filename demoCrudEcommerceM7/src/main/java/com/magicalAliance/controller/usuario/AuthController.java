package com.magicalAliance.controller.usuario;

import com.magicalAliance.dto.usuario.RegistroDTO;
import com.magicalAliance.entity.usuario.Usuario;
import com.magicalAliance.exception.MagicalBusinessException;
import com.magicalAliance.mapper.UsuarioMapper;
import com.magicalAliance.service.usuario.IUsuarioService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class AuthController {

    @Autowired
    private IUsuarioService usuarioService;

    @Autowired
    private UsuarioMapper usuarioMapper;

    // --- 1. ACCESO AL PORTAL (LOGIN) ---
    @GetMapping("/login")
    public String login(Authentication auth,
                        @RequestParam(value = "error", required = false) String error,
                        @RequestParam(value = "logout", required = false) String logout,
                        Model model) {

        // Primero reviso si quien intenta entrar ya tiene una sesión activa y real.
        // Si ya está logueado, lo redirecciono al home para que no pierda tiempo en el login.
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            return "redirect:/home";
        }

        // Si detecto un error en los parámetros (enviado por Spring Security mediante /login?error=true),
        // preparo los mensajes para mis modales mágicos.
        if (error != null) {
            model.addAttribute("error", "Credenciales incorrectas o cuenta inexistente.");
            model.addAttribute("mensajeError", "Tus credenciales no tienen suficiente maná o son incorrectas.");
        }

        // Si el usuario viene de un cierre de sesión (/login?logout), le confirmo que ha salido del reino con éxito.
        if (logout != null) {
            model.addAttribute("msg", "Has cerrado tu sesión en la Alianza correctamente.");
        }

        // CORRECCIÓN DE RUTA: templates/public/login.html
        return "public/login";
    }

    // --- 2. MANIFESTACIÓN DE NUEVAS GUARDIANAS (REGISTRO PÚBLICO) ---
    @GetMapping("/registro")
    public String mostrarRegistro(Model model) {
        // Preparo un DTO de registro vacío para que el formulario sepa qué datos recolectar.
        model.addAttribute("registroDTO", new RegistroDTO());
        // CORRECCIÓN DE RUTA: templates/public/registro.html
        return "public/registro";
    }

    @PostMapping("/registro")
    public String registrar(@Valid @ModelAttribute("registroDTO") RegistroDTO dto,
                            BindingResult result,
                            RedirectAttributes flash,
                            Model model) {

        // Primero compruebo si el formulario cumple con todas mis reglas sagradas (RUT, formato de email, etc.).
        if (result.hasErrors()) {
            return "public/registro";
        }

        try {
            // Transformo los datos del DTO en una entidad Usuario real usando mi Mapper.
            Usuario usuario = usuarioMapper.toUsuario(dto);

            // Le entrego el usuario al servicio para que lo registre (encriptando su clave y vinculando su Cliente).
            usuarioService.registrarUsuario(usuario);

            // Si todo sale bien, guardo un mensaje de éxito que sobrevivirá al redireccionamiento.
            flash.addFlashAttribute("exito", "¡Registro exitoso! Ya puedes iniciar sesión en la Alianza.");
            return "redirect:/login";

        } catch (MagicalBusinessException e) {
            // Si el servicio detecta que el RUT ya existe o es menor de edad, capturo su mensaje y te lo devuelvo.
            model.addAttribute("error", e.getMessage());
            model.addAttribute("mensajeError", e.getMessage());
            model.addAttribute("registroDTO", dto); // Mantengo tus datos escritos para que no tengas que empezar de cero.
            return "public/registro";

        } catch (Exception e) {
            // Ante cualquier interferencia oscura o error técnico, te aviso de inmediato.
            model.addAttribute("error", "Error inesperado en el reino: " + e.getMessage());
            return "public/registro";
        }
    }

    // --- 3. RECUPERACIÓN DE CONTRASEÑA ---

    @GetMapping("/recuperar-contrasena")
    public String mostrarRecuperar(Authentication auth) {
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            return "redirect:/home";
        }
        return "public/recuperar-contrasena";
    }

    @PostMapping("/recuperar-contrasena")
    public String procesarRecuperar(@RequestParam String email, RedirectAttributes flash) {
        usuarioService.iniciarRecuperacionContrasena(email);
        // Respuesta siempre positiva para no revelar si el email existe
        flash.addFlashAttribute("exito", "Si ese correo está registrado, recibirás un enlace para restablecer tu contraseña.");
        return "redirect:/recuperar-contrasena";
    }

    @GetMapping("/nueva-contrasena")
    public String mostrarNuevaContrasena(@RequestParam String token, Model model) {
        model.addAttribute("token", token);
        return "public/nueva-contrasena";
    }

    @PostMapping("/nueva-contrasena")
    public String procesarNuevaContrasena(@RequestParam String token,
                                          @RequestParam String password,
                                          @RequestParam String confirmarPassword,
                                          RedirectAttributes flash,
                                          Model model) {
        if (!password.equals(confirmarPassword)) {
            model.addAttribute("token", token);
            model.addAttribute("error", "Las contraseñas no coinciden.");
            return "public/nueva-contrasena";
        }
        try {
            usuarioService.completarRecuperacionContrasena(token, password);
            flash.addFlashAttribute("exito", "¡Contraseña actualizada! Ya puedes iniciar sesión en la Alianza.");
            return "redirect:/login";
        } catch (MagicalBusinessException e) {
            model.addAttribute("token", token);
            model.addAttribute("error", e.getMessage());
            return "public/nueva-contrasena";
        }
    }
}