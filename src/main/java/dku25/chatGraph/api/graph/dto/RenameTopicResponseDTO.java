package dku25.chatGraph.api.graph.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RenameTopicResponseDTO {
    private String topicId;
    private String topicName;
    private boolean isFavorite;
    private LocalDateTime createdAt;
} 