package dku25.chatGraph.api.openai.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@AllArgsConstructor
public class AskResponse {
    private String answer;
    private String questionId;
    private String topicId;
}
