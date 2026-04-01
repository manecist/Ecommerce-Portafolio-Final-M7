package com.magicalAliance.controller;

import com.magicalAliance.entity.Contacto;
import com.magicalAliance.service.IContactoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/contacto")
@PreAuthorize("hasRole('ADMIN')")
public class AdminContactoController {

    @Autowired
    private IContactoService contactoService;

    @GetMapping
    public String listar(Model model) {
        List<Contacto> mensajes = contactoService.listarTodos();
        model.addAttribute("mensajes", mensajes);
        return "admin/contacto/contacto-list";
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id, RedirectAttributes flash) {
        try {
            contactoService.eliminar(id);
            flash.addFlashAttribute("success", "Mensaje eliminado correctamente");
        } catch (Exception e) {
            flash.addFlashAttribute("error", "Error al eliminar el mensaje");
        }
        return "redirect:/admin/contacto";
    }
}