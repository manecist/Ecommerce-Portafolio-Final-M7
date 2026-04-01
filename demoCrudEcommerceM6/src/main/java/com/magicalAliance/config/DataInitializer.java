package com.magicalAliance.config;


import com.magicalAliance.entity.usuario.Rol;
import com.magicalAliance.repository.usuario.RolRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Autowired
    private RolRepository rolRepo;

    @Override
    @Transactional // Asegura que si falla la creación de uno, no quede inconsistente
    public void run(String... args) {
        try {
            log.info("Iniciando validación de roles fundamentales en la Alianza...");

            // 1. Crear Rol ADMINISTRADOR
            crearRolSiNoExiste("ROLE_ADMIN");

            // 2. Crear Rol CLIENTE
            crearRolSiNoExiste("ROLE_CLIENT");

            log.info("Verificación de roles completada con éxito.");

        } catch (Exception e) {
            log.error("Error místico al inicializar los datos: {}", e.getMessage());
        }
    }

    /**
     * Método privado para evitar repetir lógica y mantener el código limpio
     */
    private void crearRolSiNoExiste(String nombreRol) {
        if (rolRepo.findByNombre(nombreRol).isEmpty()) {
            rolRepo.save(new Rol(null, nombreRol));
            log.info("--> El rol {} ha sido manifestado en la base de datos.", nombreRol);
        }
    }
}