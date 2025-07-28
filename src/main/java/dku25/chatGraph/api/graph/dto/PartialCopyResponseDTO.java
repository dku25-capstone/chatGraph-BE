package dku25.chatGraph.api.graph.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class PartialCopyResponseDTO {
    private List<String> newQuestionIds;
}
