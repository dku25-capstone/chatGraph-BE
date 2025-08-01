package dku25.chatGraph.api.graph.node;

import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.data.neo4j.core.schema.*;

import java.util.List;
import java.util.UUID;

@Node("Question")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class QuestionNode extends DefaultNode {
    @Id
    @Setter(AccessLevel.NONE)
    private String questionId;
    private String text;
    private int level;

    @Relationship(type = "HAS_ANSWER", direction = Relationship.Direction.OUTGOING)
    private AnswerNode answer;

    @Relationship(type = "FOLLOWED_BY", direction = Relationship.Direction.OUTGOING)
    private QuestionNode followedBy; // 단일 필드

    public static QuestionNode createQuestion(String text, QuestionNode previousQuestion) {
        return QuestionNode.builder()
                .questionId("question-" + UUID.randomUUID())
                .level(previousQuestion != null ? previousQuestion.getLevel() + 1 : 1)
                .text(text)
                .build();
    }
}
