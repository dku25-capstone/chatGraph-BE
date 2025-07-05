package dku25.chatGraph.api.util;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Claims;

@Component
public class JwtUtil {
  @Value("${jwt.secret}")
  private String secretkey;
  @Value("${jwt.expiration}")
  private long expirationMs;

  public String generateToken(String email, String role) {
    Date now = new Date();
    Date expiryDate = new Date(now.getTime() + expirationMs);
    Key key = new SecretKeySpec(secretkey.getBytes(StandardCharsets.UTF_8), SignatureAlgorithm.HS256.getJcaName());
    return Jwts.builder()
            .setSubject(email)
            .claim("role", role)
            .setIssuedAt(now)
            .setExpiration(expiryDate)
            .signWith(key, SignatureAlgorithm.HS256)
            .compact();
  } //토큰 생성

  public String getEmailFromToken(String token) {
    return Jwts.parserBuilder()
            .setSigningKey(secretkey.getBytes(StandardCharsets.UTF_8))
            .build()
            .parseClaimsJws(token)
            .getBody()
            .getSubject();
  } // 토큰에서 이메일 추출

  public String getRoleFromToken(String token) {
    Claims claims = Jwts.parserBuilder()
        .setSigningKey(secretkey.getBytes(StandardCharsets.UTF_8))
        .build()
        .parseClaimsJws(token)
        .getBody();
    return claims.get("role", String.class);
  }// 토큰에서 사용자 권한 추출
  
  public boolean validateToken(String token) {
    try {
        Jwts.parserBuilder()
            .setSigningKey(secretkey.getBytes(StandardCharsets.UTF_8))
            .build()
            .parseClaimsJws(token);
        return true;
    } catch (JwtException | IllegalArgumentException e) {
        return false;
    }
  }// 토큰 유효성 검증

}
