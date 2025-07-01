package dku25.chatGraph.api.graph.node;

import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

@Node("Topic")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class TopicNode extends DefaultNode {
    @Id
    @Setter(AccessLevel.NONE)
    private String topicId;

    private String topicName;

}
