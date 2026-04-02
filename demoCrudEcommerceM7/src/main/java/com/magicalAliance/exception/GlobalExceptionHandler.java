package com.magicalAliance.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


@ControllerAdvice
public class GlobalExceptionHandler {

    // --- 1. OBJETO DESVANECIDO (404) ---
    @ExceptionHandler(MagicalNotFoundException.class)
    public String handleNotFound(MagicalNotFoundException ex, Model model) {
        // Preparo los mensajes para la vista cuando un habitante o producto no existe.
        model.addAttribute("errorTitulo", "Objeto Desvanecido");
        model.addAttribute("errorMensaje", ex.getMessage());

        // Corrijo la ruta: ahora apunto a la carpeta 'public/error/' donde tienes tu 404.html
        return "public/error/404";
    }

    // --- 2. REGLAS DEL REINO ROTAS (Business Error) ---
    @ExceptionHandler(MagicalBusinessException.class)
    public String handleBusiness(MagicalBusinessException ex, RedirectAttributes flash, HttpServletRequest request) {
        // Agrego el mensaje místico al flash para que se vea en el formulario tras la redirección.
        flash.addFlashAttribute("error", "🔮 " + ex.getMessage());

        // Intento devolver al usuario exactamente a la página donde estaba (el Referer).
        // Si no logro saber de dónde viene, lo envío al catálogo de productos por defecto.
        String referer = request.getHeader("Referer");
        return "redirect:" + (referer != null ? referer : "/productos");
    }

    // --- 3. PERTURBACIÓN EN LA MAGIA (500) ---
    @ExceptionHandler(Exception.class)
    public String handleGlobal(Exception ex, Model model) {
        // Capturo cualquier fallo técnico inesperado que no sea de mi lógica de negocio.
        model.addAttribute("errorTitulo", "Perturbación en la Magia");
        model.addAttribute("errorMensaje", "Algo salió mal en el servidor: " + ex.getMessage());

        // Corrijo la ruta: apunto a 'public/error/' para localizar tu 500.html
        return "public/error/500";
    }
}