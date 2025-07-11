package dku25.chatGraph.api.graph.node;

import java.util.List;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Node("User")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class UserNode extends DefaultNode {
    @Id
    private String userId;
    @Relationship(type = "OWNS", direction = Relationship.Direction.OUTGOING)
    private List<TopicNode> topics; // 1:N 관계 표현
    
    public static UserNode createUser(String userId) {
        return UserNode.builder()
                .userId(userId)
                .topics(null) // 초기에는 토픽이 없으므로 null
                .build();
    }
}