package com.magicalAliance.controller;

import com.magicalAliance.entity.Suscriptor;
import com.magicalAliance.repository.SuscriptorRepository;
import com.magicalAliance.service.IEmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import com.magicalAliance.util.PaginacionHelper;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.List;

@Controller
@RequestMapping("/admin/subscribir")
@PreAuthorize("hasRole('ADMIN')")
public class AdminSuscriptorController {

    @Autowired private SuscriptorRepository suscriptorRepository;
    @Autowired private IEmailService emailService;

    @GetMapping
    public String listar(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {
        List<Suscriptor> todos = suscriptorRepository.findAllByOrderByFechaSuscripcionDesc();
        model.addAttribute("suscriptores", PaginacionHelper.paginar(todos, page, size, model));
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