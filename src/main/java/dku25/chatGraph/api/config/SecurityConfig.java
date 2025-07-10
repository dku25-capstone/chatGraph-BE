package dku25.chatGraph.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import dku25.chatGraph.api.util.JwtAuthenticationFilter;
import dku25.chatGraph.api.util.JwtUtil;

@Configuration
public class SecurityConfig {
    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(JwtUtil jwtUtil) {
        return new JwtAuthenticationFilter(jwtUtil);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                // 회원가입/로그인 관련 페이지 및 API는 모두 허용
                .requestMatchers("/signup", "/login", "/signup.html", "/login.html", "/signup.js", "/login.js").permitAll()
                // 메인(index.html), chat.html, script.js 등 정적 파일 모두 허용
                .requestMatchers("/", "/index.html", "/chat.html", "/script.js").permitAll()
                // 내부 API만 인증 필요
                .requestMatchers("/ask-context").authenticated()
                // 그 외는 모두 인증 필요
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
            // JWt 인증 필터를 앞에 등록. JWT 토큰이 있는 요청은 "폼 로그인 인증" 을 건너뜀.
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}