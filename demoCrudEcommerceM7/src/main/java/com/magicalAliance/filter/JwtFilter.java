package com.magicalAliance.filter;

import com.magicalAliance.exception.MagicalBusinessException;
import com.magicalAliance.service.usuario.CustomUserDetailsService;
import com.magicalAliance.service.usuario.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

// once asegura que los filtro sse ejecuten una veZ por cada click
@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    // filtro que se activa cada ves que se autentica el usuario
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        try {
            // busca en el heaer la autentificación de usuario realizada
            final String authHeader = request.getHeader("Authorization");

            // si no hay autentifacion no se realiza el filtro
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                filterChain.doFilter(request, response);
                return;
            }

            // parsea la contraseña secreta donde elimina la palabra Bearer del inicio
            String token = authHeader.substring(7); // Quita "Bearer "

            // busca el usuario en este caso el email
            String email = jwtService.extractUsername(token);

            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                // el email es distinto de nulo y aun no esta auntenticado pasa...
                // Busca los detalles del usuario en la BD (roles, pass, etc) usando nuestro CustomService
                UserDetails user = userDetailsService.loadUserByUsername(email);

                // Crea la "credencial" interna de Spring Security
                // se comprueba si la contraseña es la correspondiente
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                user, null, user.getAuthorities()
                        );

                // ¡AUTORIZADO! usuario en el contexto de seguridad
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }

        } catch (Exception e) {
            // Token inválido, expirado o cualquier error: limpiamos el contexto
            // y dejamos continuar la cadena sin autenticar (Spring Security manejará el acceso)
            SecurityContextHolder.clearContext();
        }

        // si es correcto y pasa los filtros se registra
        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        // Si la ruta es una de estas, el filtro NI SE EJECUTA.
        // Esto es lo que evita la pantalla en blanco.
        return path.startsWith("/assets/") ||
                path.startsWith("/css/") ||
                path.startsWith("/js/") ||
                path.startsWith("/img/") ||
                path.startsWith("/productos/") || // <--- ESTRICTAMENTE NECESARIO PARA CARGAR IMÁGENES DE PRODUCTOS
                path.startsWith("/carrito/") ||   // Carrito usa sesión, no JWT
                path.startsWith("/login") ||
                path.startsWith("/registro") ||
                path.equals("/home") ||
                path.equals("/");
    }

}