package dku25.chatGraph.api.graph.node;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Node("User")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserNode {
    @Id
    private Long userId;
}