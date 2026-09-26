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

@Service
public class JwtService {
    private final JwtProperties properties;

    public JwtService(JwtProperties properties) { this.properties = properties; }

    public String createToken(AppUser user) {
        Instant now = Instant.now();
        return Jwts.builder().subject(user.getEmail()).claim("role", user.getRole().name())
                .issuedAt(Date.from(now)).expiration(Date.from(now.plus(properties.expirationMinutes(), ChronoUnit.MINUTES)))
                .signWith(signingKey()).compact();
    }

    public String subject(String token) { return parse(token).getPayload().getSubject(); }

    private Jws<Claims> parse(String token) {
        return Jwts.parser().verifyWith(signingKey()).build().parseSignedClaims(token);
    }

    private SecretKey signingKey() { return Keys.hmacShaKeyFor(Decoders.BASE64.decode(properties.secret())); }
}
