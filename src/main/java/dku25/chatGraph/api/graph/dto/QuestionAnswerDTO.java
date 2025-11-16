package dku25.chatGraph.api.graph.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class QuestionAnswerDTO {
    private final String questionId;
    private final String questionText;
    private final int level;
    private final boolean isFavorite;
    private final String answerId;
    private final String answerText;
    private final LocalDateTime createdAt;
    private final List<String> children;

    public QuestionAnswerDTO(String questionId, String questionText, int level,boolean isFavorite, String answerId, String answerText, LocalDateTime createdAt, List<String> children) {
        this.questionId = questionId;
        this.questionText = questionText;
        this.level = level;
        this.isFavorite = isFavorite;
        this.answerId = answerId;
        this.answerText = answerText;
        this.createdAt = createdAt;
        this.children = children;
    }
} 