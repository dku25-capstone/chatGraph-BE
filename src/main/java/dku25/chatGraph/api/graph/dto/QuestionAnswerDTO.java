package dku25.chatGraph.api.graph.dto;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class QuestionAnswerDTO {
    private final String questionId;
    private final String questionText;
    private final int level;
    private final String answerId;
    private final String answerText;
    private final LocalDateTime createdAt;

    public QuestionAnswerDTO(String questionId, String questionText, int level, String answerId, String answerText, LocalDateTime createdAt) {
        this.questionId = questionId;
        this.questionText = questionText;
        this.level = level;
        this.answerId = answerId;
        this.answerText = answerText;
        this.createdAt = createdAt;
    }
} 