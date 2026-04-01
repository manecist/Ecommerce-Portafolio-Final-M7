package com.magicalAliance.controller;

import com.magicalAliance.entity.Suscriptor;
import com.magicalAliance.repository.SuscriptorRepository;
import com.magicalAliance.service.IEmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Controller
@RequestMapping("/subscribir")
public class SuscripcionController {

    @Autowired
    private SuscriptorRepository suscriptorRepository;

    @Autowired
    private IEmailService emailService;

    @PostMapping
    @ResponseBody
    public ResponseEntity<Map<String, String>> suscribir(@RequestParam String email) {
        if (email == null || email.isBlank() || !email.contains("@")) {
            return ResponseEntity.badRequest()
                    .body(Map.of("estado", "error", "mensaje", "El correo ingresado no es válido."));
        }

        if (!suscriptorRepository.existsByEmail(email)) {
            Suscriptor nuevo = Suscriptor.builder().email(email).build();
            suscriptorRepository.save(nuevo);
        }

        try {
            emailService.enviarConfirmacionSuscripcion(email);
        } catch (Exception e) {
            // El correo falló pero igual confirmamos al usuario (ya quedó guardado)
            return ResponseEntity.ok(Map.of(
                    "estado", "ok_sin_correo",
                    "mensaje", "¡Gracias! Tu suscripción fue registrada, aunque el correo de confirmación no pudo enviarse en este momento."
            ));
        }

        return ResponseEntity.ok(Map.of(
                "estado", "ok",
                "mensaje", "¡Listo! Enviamos un correo de confirmación a <strong>" + email + "</strong>. Revisa tu bandeja de entrada."
        ));
    }

    // ─── Baja propia del suscriptor ───────────────────────────────────────────

    @GetMapping("/baja")
    public String mostrarBaja(@RequestParam(required = false) String email, Model model) {
        if (email == null || email.isBlank()) {
            return "redirect:/home";
        }
        boolean existe = suscriptorRepository.existsByEmail(email);
        model.addAttribute("email", email);
        model.addAttribute("existe", existe);
        return "public/suscripcion-baja";
    }

    @PostMapping("/baja")
    public String confirmarBaja(@RequestParam String email, RedirectAttributes flash) {
        suscriptorRepository.findByEmail(email).ifPresent(s -> {
            suscriptorRepository.delete(s);
            try { emailService.enviarConfirmacionBaja(email); } catch (Exception ignored) {}
        });
        flash.addFlashAttribute("bajaConcluida", true);
        flash.addFlashAttribute("emailBaja", email);
        return "redirect:/subscribir/baja?email=" + email;
    }
}