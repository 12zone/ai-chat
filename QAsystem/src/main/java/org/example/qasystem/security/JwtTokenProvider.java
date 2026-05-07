package org.example.qasystem.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;

@Component
public class JwtTokenProvider {

    private static final long EXPIRE_MILLIS = 24 * 60 * 60 * 1000L;

    private final SecretKey signingKey;

    public JwtTokenProvider(@Value("${app.security.jwt.secret-base64:}") String secretBase64) {
        if (!StringUtils.hasText(secretBase64)) {
            throw new IllegalStateException(
                    "缺少 app.security.jwt.secret-base64，请在 application.yml 中配置（模板见 application.example.yml；可用 openssl rand -base64 48 生成）");
        }
        byte[] keyBytes = Decoders.BASE64.decode(secretBase64.trim());
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public String createToken(String username, List<String> roles) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + EXPIRE_MILLIS);
        return Jwts.builder()
                .subject(username)
                .claim("roles", roles)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey)
                .compact();
    }

    public String getUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
