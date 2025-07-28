package dku25.chatGraph.api.graph.dto;

import lombok.Getter;

import java.util.List;

@Getter
public class PartialCopyRequestDTO {
    private List<String> sourceQuestionIds;
    private String targetParentId;
}
