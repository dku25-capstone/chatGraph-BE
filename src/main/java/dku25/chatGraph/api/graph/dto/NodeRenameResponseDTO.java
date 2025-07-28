package dku25.chatGraph.api.graph.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class NodeRenameResponseDTO {
    private final String nodeId;
    private final String nodeType;
    private final Object nodeData;
}
