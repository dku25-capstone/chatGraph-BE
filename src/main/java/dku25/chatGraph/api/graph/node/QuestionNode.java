package dku25.chatGraph.api.graph.node;

import org.springframework.data.neo4j.core.schema.*;

@Node("Question")
public class QuestionNode {
    @Id
    @GeneratedValue
    private Long id;

    private String text;
    private String sessionId;

    @Relationship(type = "HAS_ANSWER", direction = Relationship.Direction.OUTGOING)
    private AnswerNode answer;

    @Relationship(type = "NEXT", direction = Relationship.Direction.OUTGOING)
    private QuestionNode next;

    // Getters & Setters
    public Long getId() { return id; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public AnswerNode getAnswer() { return answer; }
    public void setAnswer(AnswerNode answer) { this.answer = answer; }
    public QuestionNode getNext() { return next; }
    public void setNext(QuestionNode next) { this.next = next; }
}
