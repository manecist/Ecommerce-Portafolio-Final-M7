package com.magicalAliance.service.img;

import com.magicalAliance.exception.MagicalBusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class MvcConfig implements WebMvcConfigurer {

    @Value("${app.upload.path}")
    private String uploadPath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        try {
            // 1. RECURSOS ESTÁTICOS DEL PROYECTO (CSS, JS interno)
            registry.addResourceHandler("/assets/**")
                    .addResourceLocations("classpath:/static/assets/");

            String rootPath = Paths.get(uploadPath).toAbsolutePath().toUri().toString();
            if (!rootPath.endsWith("/")) {
                rootPath += "/";
            }

            // 2. CAMBIO CLAVE: Usamos un prefijo único para archivos externos
            // Esto evita que choque con tu @GetMapping("/categorias") del controlador
            registry.addResourceHandler("/uploads/categorias/**")
                    .addResourceLocations(rootPath + "categorias/");

            registry.addResourceHandler("/uploads/productos/**")
                    .addResourceLocations(rootPath + "productos/");

        } catch (Exception e) {
            throw new MagicalBusinessException("Error al conectar los puentes de imágenes: " + e.getMessage());
        }
    }
}
