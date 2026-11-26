package com.highlands.order.security;

import com.highlands.order.config.JwtProperties;
import com.highlands.order.model.AppUser;
import com.highlands.order.model.Role;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtServiceTest {
    private static final String SECRET = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";
    private final JwtProperties properties = new JwtProperties(SECRET, 15, "issuer", "web");
    private final JwtService service = new JwtService(properties);

    @Test
    void accessTokenUsesStableIdentityAndEmailClaim() {
        AppUser user = AppUser.builder().id(42L).email("user@example.com").role(Role.CUSTOMER).build();

        String token = service.createToken(user);

        assertEquals("42", service.subject(token));
        assertEquals("user@example.com", service.email(token));
    }

    @Test
    void rejectsWrongIssuerAndTokenType() {
        String wrongIssuer = token("other", "web", "access");
        String refresh = token("issuer", "web", "refresh");

        assertThrows(JwtException.class, () -> service.email(wrongIssuer));
        assertThrows(JwtException.class, () -> service.email(refresh));
    }

    private String token(String issuer, String audience, String type) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject("42").issuer(issuer).audience().add(audience).and()
                .claim("email", "user@example.com").claim("token_type", type)
                .issuedAt(Date.from(now)).expiration(Date.from(now.plusSeconds(300)))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET))).compact();
    }
}
