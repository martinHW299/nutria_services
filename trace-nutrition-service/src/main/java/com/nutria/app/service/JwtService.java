package com.nutria.app.service;


import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.function.Function;

@Service
public class JwtService {

    @Value("${application.security.jwt.secret-key}")
    private String secretKey;

    public Long extractId(String token) {
        return extractClaim(token, claims -> claims.get("id", Long.class));
    }

    public Double extractTdee(String token) {
        return extractClaim(token, claims -> claims.get("tdee", Double.class));
    }

    public Double extractWeightGoal(String token) {
        return extractClaim(token, claims -> claims.get("weightGoal", Double.class));
    }

    public double extractBmr(String token) {
        return extractClaim(token, claims -> claims.get("bmr", double.class));
    }

    public double extractBmi(String token) {
        return extractClaim(token, claims -> claims.get("bmi", double.class));
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token.replace("Bearer ", "").trim());
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser().setSigningKey(getSignInKey()).build().parseClaimsJws(token).getBody();
    }

    private Key getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }


}
