package dku25.chatGraph.api.openai.dto;

import dku25.chatGraph.api.graph.dto.QuestionNodeMapDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

import java.util.Map;

@Getter
@ToString
public class AskResponse {
    private final String topic;
    private final Map<String, QuestionNodeMapDTO> nodes;

    public AskResponse(String topic, Map<String, QuestionNodeMapDTO> nodes) {
        this.topic = topic;
        this.nodes = nodes;
    }
}
