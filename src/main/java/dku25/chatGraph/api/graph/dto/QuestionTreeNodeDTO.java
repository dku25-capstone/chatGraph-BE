package dku25.chatGraph.api.graph.dto;

import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@ToString
public class QuestionTreeNodeDTO {
    private final String questionId;
    private final String question;
    private final int level;
    private final String answerId;
    private final String answer;
    private final LocalDateTime createdAt;
    private final List<QuestionTreeNodeDTO> children;

    public QuestionTreeNodeDTO(String questionId, String question, int level, String answerId, String answer, LocalDateTime createdAt, List<QuestionTreeNodeDTO> children) {
        this.questionId = questionId;
        this.question = question;
        this.level = level;
        this.answerId = answerId;
        this.answer = answer;
        this.createdAt = createdAt;
        this.children = children;
    }
}
