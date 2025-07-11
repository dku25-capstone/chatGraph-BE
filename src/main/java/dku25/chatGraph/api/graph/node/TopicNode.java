package dku25.chatGraph.api.graph.node;

import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

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

    @Relationship(type = "start_conversation", direction = Relationship.Direction.OUTGOING)
    private QuestionNode firstQuestion;

    @Relationship(type = "OWNS", direction = Relationship.Direction.INCOMING)
    private UserNode user;
    // user 관계 추가
    public static TopicNode createTopic(String topicName, UserNode user) {
        return TopicNode.builder()
                .topicId("topic-" + UUID.randomUUID())
                .topicName(topicName)
                .user(user) //user 파라미터
                .build();
    }
}
