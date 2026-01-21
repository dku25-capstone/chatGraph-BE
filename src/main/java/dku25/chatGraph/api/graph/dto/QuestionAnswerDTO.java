package dku25.chatGraph.api.graph.dto;

import dku25.chatGraph.api.graph.node.QuestionNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Getter
@ToString
@Builder            // 1. 빌더 패턴 자동 생성
@AllArgsConstructor // 2. 빌더가 사용할 전체 생성자 자동 생성
public class QuestionAnswerDTO {

    private final String questionId;
    private final String questionText;
    private final int level;
    private final boolean isFavorite;
    private final String answerId;
    private final String answerText;
    private final LocalDateTime createdAt;
    private final List<String> children;

    /**
     * 정적 팩토리 메서드 (Builder 사용)
     * @param node 엔티티 (본체)
     * @param childrenIds 자식 ID 리스트 (외부 주입)
     */
    public static QuestionAnswerDTO from(QuestionNode node, List<String> childrenIds) {
        if (node == null) {
            return null;
        }

        // 답변 정보 추출
        String ansId = null;
        String ansText = null;
        if (node.getAnswer() != null) {
            ansId = node.getAnswer().getAnswerId();
            ansText = node.getAnswer().getText();
        }

        // 외부에서 받은 childrenIds가 null이면 빈 리스트로 처리
        List<String> safeChildren = (childrenIds != null) ? childrenIds : Collections.emptyList();

        // 3. 여기서 Builder를 사용하여 생성! (순서 헷갈릴 걱정 없음)
        return QuestionAnswerDTO.builder()
                .questionId(node.getQuestionId())
                .questionText(node.getText())
                .level(node.getLevel())
                .isFavorite(node.isFavorite())
                .answerId(ansId)
                .answerText(ansText)
                .createdAt(node.getCreatedAt())
                .children(safeChildren) // <- 외부에서 받은 값을 여기에 넣어줌
                .build();
    }
}