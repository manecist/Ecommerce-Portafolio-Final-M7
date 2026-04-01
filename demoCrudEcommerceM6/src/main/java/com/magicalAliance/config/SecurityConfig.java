package com.magicalAliance.config;

import com.magicalAliance.filter.JwtFilter;
import jakarta.servlet.DispatcherType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    private JwtFilter jwtFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        System.out.println(">>> SecurityConfig cargado correctamente con acceso a assets");
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.FORWARD).permitAll()

                        // --- LA LLAVE MAESTRA PARA TUS ESTILOS ---
                        // Agregamos "/assets/**" para que coincida con tu carpeta física
                        .requestMatchers("/assets/**", "/css/**", "/js/**", "/img/**", "/webjars/**").permitAll()

                        // --- ACCESO PÚBLICO (HOME Y PRODUCTOS) ---
                        .requestMatchers("/", "/home", "/test").permitAll()
                        .requestMatchers("/login", "/registro").permitAll()
                        .requestMatchers("/acceso-denegado").permitAll()
                        .requestMatchers("/contacto", "/contacto/**").permitAll()
                        .requestMatchers("/subscribir", "/subscribir/baja").permitAll()

                        // --- ADMIN PRIMERO (más específico va antes del permitAll genérico) ---
                        .requestMatchers("/usuarios/admin/**").hasRole("ADMIN")
                        .requestMatchers("/productos/admin/**").hasRole("ADMIN")
                        .requestMatchers("/productos/gestionar/**").hasRole("ADMIN")
                        .requestMatchers("/admin/categorias/**").hasRole("ADMIN")
                        .requestMatchers("/admin/subcategorias/**").hasRole("ADMIN")

                        // --- ACCESO PÚBLICO A CATÁLOGO E IMÁGENES SUBIDAS ---
                        // Estas reglas van después de las de admin para que no las bloqueen
                        .requestMatchers("/productos/**").permitAll()
                        .requestMatchers("/categorias/**").permitAll()

                        // --- CARRITO (público: invitados y registrados pueden usar el carrito) ---
                        .requestMatchers("/carrito/**").permitAll()

                        // --- PEDIDOS CLIENTE ---
                        .requestMatchers("/mis-pedidos/**").hasAnyRole("CLIENT", "ADMIN")

                        // --- PEDIDOS ADMIN ---
                        .requestMatchers("/admin/pedidos/**").hasRole("ADMIN")

                        // --- CONTACTO Y SUSCRIPTORES ADMIN ---
                        .requestMatchers("/admin/contacto/**").hasRole("ADMIN")
                        .requestMatchers("/admin/subscribir/**").hasRole("ADMIN")

                        // --- TUS FILTROS DE CLIENTE/PERFIL (RESTAURADOS) ---
                        .requestMatchers("/usuarios/perfil/**").hasAnyRole("CLIENT", "ADMIN")
                        .requestMatchers("/usuarios/editar-acceso/**").hasAnyRole("CLIENT", "ADMIN")
                        .requestMatchers("/usuarios/editar-datos/**").hasAnyRole("CLIENT", "ADMIN")
                        .requestMatchers("/usuarios/direcciones/**").hasAnyRole("CLIENT", "ADMIN")
                        .requestMatchers("/usuarios/eliminar/**").hasAnyRole("CLIENT", "ADMIN")
                        .requestMatchers("/usuarios/suscripcion/**").hasAnyRole("CLIENT", "ADMIN")

                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .defaultSuccessUrl("/home", true)
                        .failureUrl("/login?error=true")
                        .permitAll()
                )
                .sessionManagement(sess -> sess
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/home")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )
                .exceptionHandling(ex -> ex
                        .accessDeniedPage("/acceso-denegado")
                );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }


    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

}