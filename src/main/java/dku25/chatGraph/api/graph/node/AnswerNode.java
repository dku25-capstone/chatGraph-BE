package dku25.chatGraph.api.graph.node;

import org.springframework.data.neo4j.core.schema.*;

@Node("Answer")
public class AnswerNode {
    @Id
    @GeneratedValue
    private Long id;

    private String text;

    // Getters & Setters
    public Long getId() { return id; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
}
