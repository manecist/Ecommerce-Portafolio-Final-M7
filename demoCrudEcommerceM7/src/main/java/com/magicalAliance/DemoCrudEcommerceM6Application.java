package com.magicalAliance;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@SpringBootApplication
public class DemoCrudEcommerceM6Application {

	public static void main(String[] args) {


        // --- BLOQUE PARA GENERAR CLAVE DE PRUEBA ---
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String miClave = "Admin.2026!"; // La clave para usar en el login como admin
        String suClave = "Usuario.2026!"; // La clave para usar en el login como usuario
        String claveEncriptada = encoder.encode(miClave);
        String claveEncriptadasu = encoder.encode(suClave);

        System.out.println("=================================================");
        System.out.println("CLAVE PARA INSERTS DE SQL:");
        System.out.println("Clave admin: " + miClave);
        System.out.println("Clave usuario: " + suClave);
        System.out.println("Hash a copiar en la BD para admin: " + claveEncriptada);
        System.out.println("Hash a copiar en la BD para usuario: " + claveEncriptadasu);
        System.out.println("=================================================");
        // ----------------------------------------------







        SpringApplication.run(DemoCrudEcommerceM6Application.class, args);
	}

}
