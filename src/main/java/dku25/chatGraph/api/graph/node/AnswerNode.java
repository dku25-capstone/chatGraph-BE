package dku25.chatGraph.api.graph.node;

import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.data.neo4j.core.schema.*;

import java.util.UUID;

@Node("Answer")
@Getter @Setter
@NoArgsConstructor(force = true)
@AllArgsConstructor
@SuperBuilder
public class AnswerNode extends DefaultNode {
    @Id
    @Setter(AccessLevel.NONE)
    private String answerId;
    @Setter(AccessLevel.NONE)
    private String text;

    public static AnswerNode create(String text){
        return AnswerNode.builder()
                .answerId("answer-" + UUID.randomUUID().toString())
                .text(text)
                .build();
    }

}
