package dku25.chatGraph.api.graph.dto;

import lombok.Getter;

import java.util.List;

@Getter
public class ShareNodesRequestDTO {
    private List<String> sourceQuestionIds; // Topic이 들어올 수도 있음
    private String targetUserId; // 상대 UserId
}
