package dku25.chatGraph.api.openai.dto;

import dku25.chatGraph.api.graph.dto.QuestionAnswerDTO;
import lombok.Getter;
import lombok.ToString;

import java.util.Map;

@Getter
@ToString
public class AskResponse {
    private final String topic;
    private final Map<String, QuestionAnswerDTO> nodes;

    public AskResponse(String topic, Map<String, QuestionAnswerDTO> nodes) {
        this.topic = topic;
        this.nodes = nodes;
    }
}
