package dku25.chatGraph.api.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class SignupRequest {
    @Email
    @NotBlank
    private String email;
    private String password; // 소셜 로그인은 null 가능
    private String provider;
    private String providerId;
}