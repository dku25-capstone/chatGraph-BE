package dku25.chatGraph.api.graph.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopicResponseDTO {
    private String topicId;
    private String topicName;
    private String createdAt;
} 