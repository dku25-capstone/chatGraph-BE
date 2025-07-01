package dku25.chatGraph.api.graph.node;

import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.data.neo4j.core.schema.*;

import java.util.UUID;

@Node("Question")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class QuestionNode extends DefaultNode {
    @Id
    @Setter(AccessLevel.NONE)
    private String questionId;

    private String text;
    private String sessionId;
    private int level;

    @Relationship(type = "HAS_ANSWER", direction = Relationship.Direction.OUTGOING)
    private AnswerNode answer;

    @Relationship(type = "FOLLOWED_BY", direction = Relationship.Direction.OUTGOING)
    private QuestionNode followedBy;

    @Relationship(type = "PREVIOUS_QUESTION", direction = Relationship.Direction.OUTGOING)
    private QuestionNode previousQuestion;

    public static QuestionNode create(String text, String sessionId, QuestionNode previousQuestion) {
        return QuestionNode.builder()
                .questionId("question-" + UUID.randomUUID().toString())
                .text(text)
                .sessionId(sessionId)
                .previousQuestion(previousQuestion)
                .level(previousQuestion != null ? previousQuestion.getLevel() + 1 : 1 )
                .build();
    }

    public void setPreviousQuestion(QuestionNode previousQuestion) {
        this.level = previousQuestion != null ? previousQuestion.getLevel() + 1 : 1;
    }
}
