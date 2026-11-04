package com.highlands.order.security;

import com.highlands.order.config.JwtProperties;
import com.highlands.order.model.AppUser;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {
    private final JwtProperties properties;

    public JwtService(JwtProperties properties) { this.properties = properties; }

    public String createToken(AppUser user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .issuer(properties.issuer())
                .audience().add(properties.audience()).and()
                .id(UUID.randomUUID().toString())
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .claim("token_type", "access")
                .issuedAt(Date.from(now)).expiration(Date.from(now.plus(properties.expirationMinutes(), ChronoUnit.MINUTES)))
                .signWith(signingKey()).compact();
    }

    public String subject(String token) { return parse(token).getPayload().getSubject(); }

    public String email(String token) { return parse(token).getPayload().get("email", String.class); }

    private Jws<Claims> parse(String token) {
        Jws<Claims> parsed = Jwts.parser()
                .requireIssuer(properties.issuer())
                .requireAudience(properties.audience())
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token);
        if (!"access".equals(parsed.getPayload().get("token_type", String.class))) {
            throw new JwtException("Invalid token type");
        }
        return parsed;
    }

    private SecretKey signingKey() { return Keys.hmacShaKeyFor(Decoders.BASE64.decode(properties.secret())); }
}
