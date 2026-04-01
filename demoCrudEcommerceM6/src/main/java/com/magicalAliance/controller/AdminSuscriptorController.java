package com.magicalAliance.controller;

import com.magicalAliance.entity.Suscriptor;
import com.magicalAliance.repository.SuscriptorRepository;
import com.magicalAliance.service.IEmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/subscribir")
@PreAuthorize("hasRole('ADMIN')")
public class AdminSuscriptorController {

    @Autowired private SuscriptorRepository suscriptorRepository;
    @Autowired private IEmailService emailService;

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("suscriptores", suscriptorRepository.findAllByOrderByFechaSuscripcionDesc());
        return "admin/suscriptores/suscriptores-list";
    }

    @PostMapping("/{id}/dar-baja")
    public String darBaja(@PathVariable Long id, RedirectAttributes flash) {
        suscriptorRepository.findById(id).ifPresent(s -> {
            try { emailService.enviarConfirmacionBaja(s.getEmail()); } catch (Exception ignored) {}
            suscriptorRepository.delete(s);
            flash.addFlashAttribute("success", "Suscriptor " + s.getEmail() + " dado de baja");
        });
        if (!flash.getFlashAttributes().containsKey("success")) {
            flash.addFlashAttribute("error", "No se encontró el suscriptor");
        }
        return "redirect:/admin/subscribir";
    }
}