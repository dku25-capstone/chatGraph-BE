package dku25.chatGraph.api.security;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtUtil jwtUtil;

    @Transactional
    public RefreshToken createRefreshToken(String userId) {
        // 기존 refresh token이 있으면 삭제
        refreshTokenRepository.findByUserId(userId).ifPresent(refreshTokenRepository::delete);

        String tokenString = jwtUtil.generateRefreshToken(userId);
        LocalDateTime expiryDate = LocalDateTime.now()
                .plusSeconds(jwtUtil.getRefreshExpirationMs() / 1000);

        RefreshToken refreshToken = RefreshToken.builder()
                .token(tokenString)
                .userId(userId)
                .expiryDate(expiryDate)
                .createdAt(LocalDateTime.now())
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.isExpired()) {
            refreshTokenRepository.delete(token);
            throw new RuntimeException("Refresh token이 만료되었습니다. 다시 로그인해주세요.");
        }
        return token;
    }

    @Transactional
    public void deleteByUserId(String userId) {
        refreshTokenRepository.deleteByUserId(userId);
    }

    @Transactional
    public void deleteExpiredTokens() {
        refreshTokenRepository.deleteByExpiryDateBefore(LocalDateTime.now());
    }
}
