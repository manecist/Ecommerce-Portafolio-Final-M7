package com.magicalAliance.service.usuario;

import com.magicalAliance.entity.usuario.Usuario;
import com.magicalAliance.exception.MagicalBusinessException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;

// json web token
// para autenticacion y autorizacion
@Service
public class JwtService {

    @Value("${app.jwt.secret}")
    private String SECRET_KEY;

    // token funciona como credencial de paso
    public String generateToken(Usuario usuario) {
        return Jwts.builder()
                // paso email
                .setSubject(usuario.getEmail())
                // fecha de creacion token
                .setIssuedAt(new Date())
                // fecha de expiracion de token con 1 hora de duracion por sesion
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60))
                // firma el token con la clave secreta
                .signWith(getKey(), SignatureAlgorithm.HS256)
                // compacta y lo crea el token
                .compact();
    }

    // es el proceso de extraer la informacion del usuario
    // el token tiene mi informacion del usuario
    public String extractUsername(String token) {
        return getClaims(token).getSubject();
    }

    // datos que van en el token, el correo, las fechas
    // contiene la información del usuario
    private Claims getClaims(String token) {
        try {
            // crea un analizador de token
            return Jwts.parserBuilder()
                    // validando integridad del token
                    .setSigningKey(getKey())
                    // crea la conexion compacta y segura
                    .build()
                    // verifica la firma del token
                    .parseClaimsJws(token)
                    // obtiene los claims del token o datos de usuario
                    .getBody();
        } catch (ExpiredJwtException e) {
            // AJUSTE: Si el token expiró, lanzamos una excepción de negocio
            throw new MagicalBusinessException("Tu sesión mágica ha expirado. Por favor, vuelve a ingresar.");
        } catch (SignatureException | IllegalArgumentException e) {
            // AJUSTE: Si el token es falso o fue manipulado
            throw new MagicalBusinessException("Credencial mágica inválida o alterada.");
        }
    }

    // codifica la clave secreta
    private Key getKey() {
        return Keys.hmacShaKeyFor(SECRET_KEY.getBytes());
    }
}