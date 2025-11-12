package dku25.chatGraph.api.user.dto;

import lombok.Getter;
import lombok.Setter;

import jakarta.validation.constraints.NotBlank;

@Getter
@Setter
public class RefreshTokenRequest {

    @NotBlank(message = "Refresh token은 필수입니다.")
    private String refreshToken;
}
