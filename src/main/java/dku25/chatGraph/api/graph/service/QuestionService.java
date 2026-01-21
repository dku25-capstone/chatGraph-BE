package dku25.chatGraph.api.graph.service;

import dku25.chatGraph.api.exception.ResourceNotFoundException;
import dku25.chatGraph.api.graph.dto.MoveToNewTopicResponseDTO;
import dku25.chatGraph.api.graph.dto.QuestionAnswerDTO;
import dku25.chatGraph.api.graph.dto.TopicTreeMapResponseDTO;
import dku25.chatGraph.api.graph.node.QuestionNode;
import dku25.chatGraph.api.graph.node.TopicNode;
import dku25.chatGraph.api.graph.node.UserNode;
import dku25.chatGraph.api.graph.repository.QuestionRepository;
import dku25.chatGraph.api.graph.repository.TopicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // ✅ 기본적으로 읽기 전용 (조회 성능 향상)
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final TopicRepository topicRepository;
    private final NodeUtilService nodeUtilService;
    private final UserNodeService userNodeService;

    // ✅ 매직 스트링 방지용 상수 (오타 방지)
    private static final String KEY_TOPIC_ID = "topicId";
    private static final String KEY_QUESTION_NODE = "q";

    // 1. 검색 (Map 활용 & N+1 해결)
    public List<TopicTreeMapResponseDTO> searchByKeyword(String keyword, String userId) {
        // 1. DB 조회
        List<Map<String, Object>> results = questionRepository.findQuestionsWithTopicId(keyword, userId);

        // 2. 그룹화 (상수 사용 및 메서드 참조 활용)
        Map<String, List<QuestionAnswerDTO>> grouped = results.stream()
                .collect(Collectors.groupingBy(
                        row -> (String) row.get(KEY_TOPIC_ID),  // "topicId" 대신 상수 사용
                        Collectors.mapping(this::convertToDto, Collectors.toList())
                ));

        // 3. 트리 구조 변환
        return grouped.entrySet().stream()
                .map(entry -> nodeUtilService.buildMapFromFlatList(entry.getValue(), entry.getKey(), false))
                .collect(Collectors.toList());
    }

    // 2. 질문 삭제 (데이터 변경이므로 쓰기 트랜잭션 허용)
    @Transactional
    public void deleteQuestionNode(List<String> questionIds, String userId) {
        questionIds.forEach(id -> {
            nodeUtilService.checkOwnership(id, userId);
            questionRepository.deleteAndRelink(id);
        });
    }

    // 3. 질문 복제
    @Transactional
    public List<String> copyQuestionNodes(List<String> sourceQuestionIds, String targetParentId, String userId) {
        nodeUtilService.checkOwnership(targetParentId, userId);
        sourceQuestionIds.forEach(srcId -> nodeUtilService.checkOwnership(srcId, userId));

        return questionRepository.copyPartialQuestionTree(sourceQuestionIds, targetParentId);
    }

    // 4. 새 토픽으로 이동
    @Transactional
    public MoveToNewTopicResponseDTO moveToNewTopic(List<String> sourceQuestionIds, String userId) {
        UserNode currentUser = userNodeService.getUserById(userId);

        CopiedTopicResult result = createTopicAndCopyTree(sourceQuestionIds, currentUser, userId);

        // ※ 이동이 목적일 경우 아래 주석 해제 (원본 삭제)
        // deleteQuestionNode(sourceQuestionIds, userId);

        return MoveToNewTopicResponseDTO.builder()
                .newTopicId(result.topicId)
                .newQuestionIds(result.createdQuestionIds)
                .build();
    }

    // 5. 질문 공유
    @Transactional
    public void shareQuestionNodes(List<String> sourceQuestionIds, String targetUserEmail, String userId) {
        UserNode targetUser = userNodeService.getUserByEmailId(targetUserEmail);
        createTopicAndCopyTree(sourceQuestionIds, targetUser, userId);
    }

    // 6. 좋아요 토글
    @Transactional
    public void favoriteQuestionNode(String questionId, String userId) {
        nodeUtilService.checkOwnership(questionId, userId);
        questionRepository.findById(questionId).ifPresent(question -> {
            question.setFavorite(!question.isFavorite());
            questionRepository.save(question);
        });
    }

    // --- Helper Wrapper Methods ---

    public QuestionNode createQuestionNode(String prompt, QuestionNode previousQuestion) {
        return QuestionNode.createQuestion(prompt, previousQuestion);
    }

    // save를 호출하므로 Transactional 추가 필요
    @Transactional
    public void linkFollowedByToQuestion(QuestionNode previousQuestion, QuestionNode currentQuestion) {
        previousQuestion.setFollowedBy(currentQuestion);
        questionRepository.save(previousQuestion);
    }

    // --- Private Helper Methods ---

    // DTO 변환 로직 분리 (상수 사용)
    private QuestionAnswerDTO convertToDto(Map<String, Object> row) {
        // "q" 대신 상수 사용
        QuestionNode node = (QuestionNode) row.get(KEY_QUESTION_NODE);
        return QuestionAnswerDTO.from(node, Collections.emptyList());
    }

    private record CopiedTopicResult(String topicId, List<String> createdQuestionIds) {}

    private CopiedTopicResult createTopicAndCopyTree(List<String> sourceQuestionIds, UserNode newOwner, String requesterUserId) {
        if (sourceQuestionIds == null || sourceQuestionIds.isEmpty()) {
            throw new IllegalArgumentException("질문 ID 목록이 비어있습니다.");
        }

        sourceQuestionIds.forEach(id -> nodeUtilService.checkOwnership(id, requesterUserId));

        String rootQuestionId = sourceQuestionIds.get(0);
        String newTopicName = questionRepository.findById(rootQuestionId)
                .map(QuestionNode::getText)
                .orElseThrow(() -> new ResourceNotFoundException("질문을 찾을 수 없습니다."));

        TopicNode newTopic = TopicNode.createTopic(newTopicName, newOwner);
        topicRepository.save(newTopic);

        List<String> newQuestionIds = questionRepository.copyPartialQuestionTree(sourceQuestionIds, newTopic.getTopicId());

        return new CopiedTopicResult(newTopic.getTopicId(), newQuestionIds);
    }
}