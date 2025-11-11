package dku25.chatGraph.api.graph.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RenameQuestionResponseDTO {
    private String questionId;
    private String text;
    private LocalDateTime createdAt;
} 