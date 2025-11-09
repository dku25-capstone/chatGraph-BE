package dku25.chatGraph.api.user.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginResponse { // 로그인 결과 반환
    private String token;
    private String refreshToken;
    private String message;
} 