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
                .claim("id", user.getId())
                .claim("firstName", user.getFirstName())
                .claim("lastName", user.getLastName())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes()))
                .compact();
    }

    // Extract username with better error handling
    public String extractUsername(String token){
        try {
            if (token == null || token.trim().isEmpty()) {
                System.err.println("Token is null or empty");
                return null;
            }

            return Jwts.parserBuilder()
                    .setSigningKey(Keys.hmacShaKeyFor(SECRET.getBytes()))
                    .build()
                    .parseClaimsJws(token)
                    .getBody()
                    .getSubject();
        } catch (io.jsonwebtoken.MalformedJwtException e) {
            System.err.println("Malformed JWT token: " + e.getMessage());
            System.err.println("Token received: " + (token.length() > 50 ? token.substring(0, 50) + "..." : token));
            return null;
        } catch (Exception e) {
            System.err.println("Error extracting username: " + e.getMessage());
            return null;
        }
    }

    // Extract expiration date
    public Date extractExpiration(String token) {
        try {
            return extractAllClaims(token).getExpiration();
        } catch (Exception e) {
            return null;
        }
    }

    // Check if token is expired
    private Boolean isTokenExpired(String token) {
        Date expiration = extractExpiration(token);
        return expiration != null && expiration.before(new Date());
    }

    // Validate token
    public Boolean validateToken(String token, UserDetails userDetails) {
        try {
            final String username = extractUsername(token);
            return (username != null && username.equals(userDetails.getUsername()) && !isTokenExpired(token));
        } catch (Exception e) {
            System.err.println("Token validation error: " + e.getMessage());
            return false;
        }
    }

    public String extractRole(String token) {
        try {
            Claims claims = extractAllClaims(token);
            Object roleClaim = claims.get("role");
            return roleClaim != null ? roleClaim.toString() : null;
        } catch (Exception e) {
            System.err.println("Error extracting role: " + e.getMessage());
            return null;
        }
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