package dku25.chatGraph.api.graph.dto;

import lombok.Getter;

import java.util.List;

@Getter
public class MoveToNewTopicRequestDTO {
    private List<String> sourceQuestionIds;  // 이동할 서브트리의 질문 ID 목록 (첫 번째가 최상위 노드)
}
