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
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final TopicRepository topicRepository;
    private final NodeUtilService nodeUtilService;
    private final UserNodeService userNodeService;

    // 1. 검색 (Map 활용 & N+1 해결)
    @Transactional(readOnly = true)
    public List<TopicTreeMapResponseDTO> searchByKeyword(String keyword, String userId) {
        // 1. DB 조회 (Map 리스트)
        List<Map<String, Object>> results = questionRepository.findQuestionsWithTopicId(keyword, userId);

        // 2. 그룹화 (들여쓰기 대폭 감소)
        Map<String, List<QuestionAnswerDTO>> grouped = results.stream().collect(Collectors.groupingBy(row -> (String) row.get("topicId"),       // Key: TopicId 추출
                Collectors.mapping(this::convertToDto, Collectors.toList()) // Value: 변환 로직 위임
        ));

        // 3. 트리 구조 변환 (기존과 동일)
        return grouped.entrySet().stream().map(entry -> nodeUtilService.buildMapFromFlatList(entry.getValue(), entry.getKey(), false)).collect(Collectors.toList());
    }


    // 2. 질문 삭제
    @Transactional
    public void deleteQuestionNode(List<String> questionIds, String userId) {
        questionIds.forEach(id -> {
            nodeUtilService.checkOwnership(id, userId);
            questionRepository.deleteAndRelink(id);
        });
    }

    // 3. 질문 복제 (같은 트리 내 복제)
    @Transactional
    public List<String> copyQuestionNodes(List<String> sourceQuestionIds, String targetParentId, String userId) {
        nodeUtilService.checkOwnership(targetParentId, userId);
        sourceQuestionIds.forEach(srcId -> nodeUtilService.checkOwnership(srcId, userId));

        return questionRepository.copyPartialQuestionTree(sourceQuestionIds, targetParentId);
    }

    // 4. 새 토픽으로 이동 (복제 + 원본 삭제는 선택사항)
    @Transactional
    public MoveToNewTopicResponseDTO moveToNewTopic(List<String> sourceQuestionIds, String userId) {
        UserNode currentUser = userNodeService.getUserById(userId);

        // 공통 로직 사용 (내 계정으로 복제)
        CopiedTopicResult result = createTopicAndCopyTree(sourceQuestionIds, currentUser, userId);

        // ※ '이동'이 목적이라면 원본 삭제 주석 해제
        // deleteQuestionNode(sourceQuestionIds, userId);

        return MoveToNewTopicResponseDTO.builder().newTopicId(result.topicId).newQuestionIds(result.createdQuestionIds).build();
    }

    // 5. 질문 공유 (다른 사람 계정으로 복제)
    @Transactional
    public void shareQuestionNodes(List<String> sourceQuestionIds, String targetUserEmail, String userId) {
        UserNode targetUser = userNodeService.getUserByEmailId(targetUserEmail);

        // 공통 로직 사용 (상대방 계정으로 복제)
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

    public void linkFollowedByToQuestion(QuestionNode previousQuestion, QuestionNode currentQuestion) {
        previousQuestion.setFollowedBy(currentQuestion);
        questionRepository.save(previousQuestion);
    }

    // --- Private Helper Methods (중복 제거 핵심) ---

    // 복잡한 변환 로직을 담당하는 도우미 메서드
    private QuestionAnswerDTO convertToDto(Map<String, Object> row) {
        // Map에서 Node 꺼내기 (형변환 로직 숨기기)
        QuestionNode node = (QuestionNode) row.get("q");

        // DTO 변환 (검색 결과이므로 자식은 Empty)
        return QuestionAnswerDTO.from(node, Collections.emptyList());
    }

    // 내부 데이터 전달용 Record (Java 16+)
    private record CopiedTopicResult(String topicId, List<String> createdQuestionIds) {
    }

    /**
     * 공통 로직: 질문 목록을 기반으로 새 토픽을 생성하고 트리를 복제함
     */
    private CopiedTopicResult createTopicAndCopyTree(List<String> sourceQuestionIds, UserNode newOwner, String requesterUserId) {
        if (sourceQuestionIds == null || sourceQuestionIds.isEmpty()) {
            throw new IllegalArgumentException("질문 ID 목록이 비어있습니다.");
        }

        // 1. 요청자가 원본 질문들의 소유자인지 확인
        sourceQuestionIds.forEach(id -> nodeUtilService.checkOwnership(id, requesterUserId));

        // 2. 새 토픽 이름 결정 (첫 번째 질문 텍스트)
        String rootQuestionId = sourceQuestionIds.get(0);
        String newTopicName = questionRepository.findById(rootQuestionId).map(QuestionNode::getText).orElseThrow(() -> new ResourceNotFoundException("질문을 찾을 수 없습니다."));

        // 3. 새 토픽 생성
        TopicNode newTopic = TopicNode.createTopic(newTopicName, newOwner);
        topicRepository.save(newTopic);

        // 4. 트리 복제 실행
        List<String> newQuestionIds = questionRepository.copyPartialQuestionTree(sourceQuestionIds, newTopic.getTopicId());

        return new CopiedTopicResult(newTopic.getTopicId(), newQuestionIds);
    }
}