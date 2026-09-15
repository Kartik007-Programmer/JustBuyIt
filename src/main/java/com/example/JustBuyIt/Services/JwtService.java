package com.example.JustBuyIt.Services;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class JwtService {

    String Secret_key = "kicYck35fjuHfNGM30KHDJaBySUc04hiB4MnBgAjamU";

    String generateToken(UserDetails userDetails) {

        String role = userDetails
                .getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));
        System.out.println("role: " + role);
        return  generateToken(userDetails.getUsername(), role);
    }

    String generateToken(String username, String role) {
        Map<String,Object> claims = new HashMap<>();

        claims.put("role", role);
        claims.put("username", username);

        return Jwts
                .builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60)) // 1 Hour
                .signWith(getSecretKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    private Key getSecretKey() {
        byte[] keybytes = Decoders.BASE64.decode(this.Secret_key);
        return Keys.hmacShaKeyFor(keybytes);
    }

    public String getUsernameByToken(String authToken) {
        return ExtractClaims(authToken,Claims::getSubject);
    }

    public <T> T ExtractClaims(String token, Function<Claims,T> ClaimsResolver) {
        Claims claims = ExtractAllClaims(token);
        return ClaimsResolver.apply(claims);

    }

    private Claims ExtractAllClaims(String token) {
        return Jwts
                .parserBuilder()
                .setSigningKey(getSecretKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public boolean isTokenValid(String authToken, UserDetails userDetails) {
        String username = userDetails.getUsername();
        return username.equals(getUsernameByToken(authToken)) && !isTokenExpiration(authToken);
    }

    private boolean isTokenExpiration(String authToken) {
        return ExtractClaims(authToken,Claims::getExpiration).before(new Date());
    }
}
