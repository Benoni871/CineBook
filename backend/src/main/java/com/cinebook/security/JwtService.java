package com.cinebook.security;

import com.cinebook.entity.Role;
import com.cinebook.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

/** Issues and validates stateless JWTs used for authentication. */
@Service
public class JwtService {

    private final SecretKey key;
    private final long expirationMs;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms}") long expirationMs) {
        this.key = Keys.hmacShaKeyFor(io.jsonwebtoken.io.Decoders.BASE64.decode(secret));
        this.expirationMs = expirationMs;
    }

    /** Mint a token whose subject is the username, carrying the user's id, role and theaterId. */
    public String generateToken(User user) {
        Date now = new Date();
        return Jwts.builder()
                .subject(user.getUsername())
                .claim("userId", user.getId())
                .claim("role", user.getRole().name())
                .claim("theaterId", user.getTheaterId())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .signWith(key)
                .compact();
    }

    /** Parse and verify a token, returning its claims. Throws if invalid/expired. */
    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public AuthPrincipal toPrincipal(Claims claims) {
        Long userId = claims.get("userId", Number.class).longValue();
        Role role = Role.valueOf(claims.get("role", String.class));
        Number theaterId = claims.get("theaterId", Number.class);
        return new AuthPrincipal(
                userId,
                claims.getSubject(),
                role,
                theaterId == null ? null : theaterId.longValue());
    }
}
