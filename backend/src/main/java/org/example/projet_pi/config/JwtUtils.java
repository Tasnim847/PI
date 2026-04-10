package org.example.projet_pi.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.example.projet_pi.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class JwtUtils {

    @Value("${jwt.secret}")
    private String SECRET;

    @Value("${jwt.expiration}")
    private long EXPIRATION_TIME;

    // Generate token
    public String generateToken(User user){

        return Jwts.builder()
                .setSubject(user.getEmail())
                .claim("role", user.getRole())
                .claim("firstName", user.getFirstName())
                .claim("lastName", user.getLastName())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes()))
                .compact();
    }

    // Extract username
    public String extractUsername(String token){

        return Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(SECRET.getBytes()))
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }
    // Extract expiration date
    public Date extractExpiration(String token) {
        return extractAllClaims(token).getExpiration();
    }
    // Check if token is expired
    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }
    // Validate token
    public Boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }
    public String extractRole(String token) {
        Claims claims = extractAllClaims(token);
        Object roleClaim = claims.get("role");
        return roleClaim != null ? roleClaim.toString() : null;
    }
    // Extract all claims
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(SECRET.getBytes()))
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

}