package dku25.chatGraph.api.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtUtil {
  @Value("${jwt.secret}")
  private String secretkey;
  @Value("${jwt.expiration}")
  private long expirationMs;
}
