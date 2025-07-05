package dku25.chatGraph.api.user.service;

import dku25.chatGraph.api.user.domain.User;
import dku25.chatGraph.api.user.repository.UserRepository;
import dku25.chatGraph.api.user.dto.SignupRequest;
import dku25.chatGraph.api.user.dto.LoginRequest;
import dku25.chatGraph.api.user.dto.LoginResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import dku25.chatGraph.api.util.JwtUtil;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Autowired
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @Transactional
    public void signup(SignupRequest request) {
        // 이메일 중복 체크
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        String encodedPassword = null;
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            encodedPassword = passwordEncoder.encode(request.getPassword());
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(encodedPassword); // 소셜 로그인은 null
        user.setProvider(request.getProvider());
        user.setProviderId(request.getProviderId());
        user.setRole("USER");

        userRepository.save(user);
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
        return jwtUtil.generateToken(user.getEmail(), user.getRole());
    }
} 