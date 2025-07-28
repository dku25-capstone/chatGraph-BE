package dku25.chatGraph.api.graph.dto;

import lombok.Getter;
import lombok.ToString;

import java.util.Map;

@Getter
@ToString
public class TopicTreeMapResponseDTO {
    private final String topic;
    private final Map<String, Object> nodes; // ID -> TopicNodeDTO or QuestionAnswerDTO

    public TopicTreeMapResponseDTO(String topic, Map<String, Object> nodes) {
        this.topic = topic;
        this.nodes = nodes;
    }
}
