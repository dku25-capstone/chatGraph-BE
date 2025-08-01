package dku25.chatGraph.api.user.service;

import dku25.chatGraph.api.user.domain.User;
import dku25.chatGraph.api.user.repository.UserRepository;
import dku25.chatGraph.api.user.dto.SignupRequest;
import dku25.chatGraph.api.user.dto.LoginRequest;
import dku25.chatGraph.api.user.dto.LoginResponse;

import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

import dku25.chatGraph.api.security.JwtUtil;


@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    // JPA 트랜잭션 (User 저장만 담당)
    @Transactional
    public User saveUser(SignupRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        String encodedPassword = null;
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            encodedPassword = passwordEncoder.encode(request.getPassword());
        }
        
        String userId = "user-" + UUID.randomUUID();

        User user = new User();
        user.setUserId(userId);
        user.setEmail(request.getEmail());
        user.setPassword(encodedPassword);
        user.setProvider(request.getProvider());
        user.setProviderId(request.getProviderId());
        user.setRole("USER");

        return userRepository.save(user);
    }

    public LoginResponse login(LoginRequest request) {
        // 이메일로 사용자 조회
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("이메일 또는 비밀번호가 잘못되었습니다."));

        // 비밀번호 검증
        if (user.getPassword() == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("이메일 또는 비밀번호가 잘못되었습니다.");
        }

        // JWT 토큰 생성
        String token = generateJwtToken(user);

        return new LoginResponse(token, "로그인 성공");
    }

    private String generateJwtToken(User user) {
        return jwtUtil.generateToken(user.getUserId(), user.getRole());
    }
} 