package dku25.chatGraph.api.graph.node;

import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.Optional;
import java.util.UUID;

@Node("Topic")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class TopicNode extends DefaultNode {
    @Id
    @Setter(AccessLevel.NONE)
    private String topicId;
    private String topicName;
    private String sessionId;

    @Relationship(type="start_conversation", direction = Relationship.Direction.OUTGOING)
    private QuestionNode firstQuestion;

    public static TopicNode createTopic(String topicName, String sessionId) {
        return TopicNode.builder()
                .topicId("topic-" + UUID.randomUUID())
                .topicName(topicName)
                .sessionId(sessionId)
                .build();
    }
}
