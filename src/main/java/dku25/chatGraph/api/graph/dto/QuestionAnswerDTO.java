package dku25.chatGraph.api.graph.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class QuestionAnswerDTO {
    private final String questionId;
    private final String question;
    private final int level;
    private final String answerId;
    private final String answer;
    private final LocalDateTime createdAt;
    private final List<String> children;

    public QuestionAnswerDTO(String questionId, String question, int level, String answerId, String answer, LocalDateTime createdAt, List<String> children) {
        this.questionId = questionId;
        this.question = question;
        this.level = level;
        this.answerId = answerId;
        this.answer = answer;
        this.createdAt = createdAt;
        this.children = children;
    }
} 