package dku25.chatGraph.api.graph.dto;

import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@ToString
public class TopicNodeDTO {
    private final String topicId;
    private final String topicName;
    private final LocalDateTime createdAt;
    private final List<String> children;

    public TopicNodeDTO(String topicId, String topicName, LocalDateTime createdAt, List<String> children) {
        this.topicId = topicId;
        this.topicName = topicName;
        this.createdAt = createdAt;
        this.children = children;
    }
}
