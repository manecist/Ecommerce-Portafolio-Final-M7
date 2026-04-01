package com.magicalAliance.service.usuario;


import com.magicalAliance.entity.usuario.Usuario;
import com.magicalAliance.exception.MagicalNotFoundException;
import com.magicalAliance.repository.usuario.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UsuarioRepository usuarioRepo;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        // Busco al habitante por su email. Si no lo encuentro, lanzo tu propia excepción
        // MagicalNotFoundException. Esto hará que mi GlobalExceptionHandler
        // lo detecte y muestre la página "Objeto Desvanecido".
        Usuario usuario = usuarioRepo.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("No se encontró ningún usuario con el email: " + email));

        // Al encontrarlo, extraigo su esencia y la transformo al formato que el gran
        // guardián (Spring Security) requiere para otorgarle acceso.
        return User.builder()
                .username(usuario.getEmail())
                .password(usuario.getPassword())
                // Asigno el nombre del rol (como ROLE_ADMIN) para que el sistema sepa qué puertas puede abrir.
                .authorities(usuario.getRol().getNombre())
                .build();
    }
}