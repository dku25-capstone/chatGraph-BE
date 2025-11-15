package dku25.chatGraph.api.graph.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class MoveToNewTopicResponseDTO {
    private String newTopicId;                // 새로 생성된 토픽 ID
    private List<String> newQuestionIds;      // 새로 복제된 질문 ID 목록
}
