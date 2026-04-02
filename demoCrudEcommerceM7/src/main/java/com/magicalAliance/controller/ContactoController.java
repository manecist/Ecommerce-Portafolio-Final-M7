package com.magicalAliance.controller;

import com.magicalAliance.entity.Contacto;
import com.magicalAliance.service.IContactoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/contacto")
public class ContactoController {

    @Autowired
    private IContactoService contactoService;

    @GetMapping
    public String mostrarFormulario(Model model) {
        model.addAttribute("contacto", new Contacto());
        return "public/contacto";
    }

    @PostMapping
    public String enviarMensaje(@Valid @ModelAttribute("contacto") Contacto contacto,
                                BindingResult result,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "public/contacto";
        }

        try {
            contactoService.guardar(contacto);
            redirectAttributes.addFlashAttribute("exito", "¡Mensaje enviado! Nos pondremos en contacto contigo pronto.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Hubo un problema al enviar tu mensaje. Intenta de nuevo.");
        }

        return "redirect:/contacto";
    }
}