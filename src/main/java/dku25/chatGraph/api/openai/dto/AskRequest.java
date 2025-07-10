package dku25.chatGraph.api.openai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class AskRequest {
    @NotBlank(message = "prompt는 비어 있을 수 없습니다.")
    private String prompt;
    private String previousQuestionId;

}
