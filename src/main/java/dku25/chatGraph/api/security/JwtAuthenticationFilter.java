package dku25.chatGraph.api.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.List;

public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    //FilterChain : 현재 필터가 끝나면, 다음필터 or 컨트롤러로 요청을 넘김 (요청이 흐름을 이어감)
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        // 요청 헤더에서 토큰 추출
        String authHeader = request.getHeader("Authorization");
        String token = null;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7); // "Bearer " 이후의 문자열만 추출
        }

        // 토큰 검증 및 인증 정보 설정
        if (token != null && jwtUtil.validateToken(token)) {
            // 1. 토큰에서 사용자 정보 추출
            String userId = jwtUtil.getUserIdFromToken(token);
            String role = jwtUtil.getRoleFromToken(token); // (role도 claim에서 추출 가능하다면 추출)

            CustomUserDetails userDetails = new CustomUserDetails(userId, role);

            // 2. 인증 객체 생성 (role은 단일 권한 예시)
            UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

            // 3. SecurityContext에 인증 정보 저장
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path   = request.getServletPath();
        String method = request.getMethod();

        // POST /signup, POST /login, 그리고 모든 OPTIONS 요청은 JWT 검증 스킵
        if (HttpMethod.OPTIONS.matches(method)) return true;
        if ("/signup".equals(path) && HttpMethod.POST.matches(method)) return true;
        if ("/login".equals(path)  && HttpMethod.POST.matches(method)) return true;
        return false;
    }
} 