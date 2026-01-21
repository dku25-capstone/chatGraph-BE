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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QuestionService {
    private final QuestionRepository questionRepository;
    private final TopicRepository topicRepository;
    private final NodeUtilService nodeUtilService;
    private final UserNodeService userNodeService;

    public QuestionService(QuestionRepository questionRepository, TopicRepository topicRepository,
                           NodeUtilService nodeUtilService, UserNodeService userNodeService) {
        this.questionRepository = questionRepository;
        this.topicRepository = topicRepository;
        this.nodeUtilService = nodeUtilService;
        this.userNodeService = userNodeService;
    }

    // Query Parameter로 QuestionNode 조회
    public List<TopicTreeMapResponseDTO> searchByKeyword(String keyword, String userId) {
        List<QuestionAnswerDTO> questionNodes = findQuestionByKeyword(keyword, userId);

        Map<String, List<QuestionAnswerDTO>> groupedByTopic = groupBasedTopicId(questionNodes);

        return convertTopicTreeMapResult(groupedByTopic);
    }

    // QuestionNode 생성
    public QuestionNode createQuestionNode(String prompt, QuestionNode previousQuestion) {
        return QuestionNode.createQuestion(prompt, previousQuestion);
    }

    // QuestionNode간 FollowedBy 관계 생성
    public void linkFollowedByToQuestion(QuestionNode previousQuestion, QuestionNode currentQuestion) {
        previousQuestion.setFollowedBy(currentQuestion);
        questionRepository.save(previousQuestion);
    }

    // 질문 노드(단일, 복수) 삭제 -> 이에 따른 답변 노드도 삭제
    @Transactional
    public void deleteQuestionNode(List<String> questionIds, String userId) {
        // 상위 노드가 질문 노드인지, 토픽 노드인지 판별
        for (String questionId : questionIds) {
            nodeUtilService.checkOwnership(questionId, userId);
            questionRepository.deleteAndRelink(questionId);
        }
    }

    // 질문 노드(단일, 복수) 복제
    @Transactional
    public List<String> copyQuestionNodes(List<String> sourceQuestionIds, String targetParentId, String userId) {
        // 권한 체크
        nodeUtilService.checkOwnership(targetParentId, userId);

        for (String srcId : sourceQuestionIds) {
            nodeUtilService.checkOwnership(srcId, userId);
        }

        return questionRepository.copyPartialQuestionTree(sourceQuestionIds, targetParentId);
    }

    // 서브트리를 새로운 토픽으로 이동 (복제 + 원본 삭제)
    @Transactional
    public MoveToNewTopicResponseDTO moveToNewTopic(List<String> sourceQuestionIds, String userId) {
        // 1. 권한 체크
        for (String srcId : sourceQuestionIds) {
            nodeUtilService.checkOwnership(srcId, userId);
        }

        // 2. 최상위 노드의 text를 새 토픽 이름으로 사용
        // (첫 번째 질문이 최상위 노드라고 가정)
        String rootQuestionId = sourceQuestionIds.get(0);
        QuestionNode rootQuestion = questionRepository.findById(rootQuestionId)
                .orElseThrow(() -> new ResourceNotFoundException("질문을 찾을 수 없습니다."));
        String newTopicName = rootQuestion.getText();

        // 3. 새 토픽 생성
        UserNode user = userNodeService.getUserById(userId);
        TopicNode newTopic = TopicNode.createTopic(newTopicName, user);
        topicRepository.save(newTopic);

        // 4. 서브트리를 새 토픽으로 복제 (최상위 노드가 토픽의 첫 질문이 됨)
        List<String> newQuestionIds = questionRepository.copyPartialQuestionTree(sourceQuestionIds,
                newTopic.getTopicId());

        // 5. 응답 생성
        return MoveToNewTopicResponseDTO.builder()
                .newTopicId(newTopic.getTopicId())
                .newQuestionIds(newQuestionIds)
                .build();
    }

    @Transactional
    public void shareQuestionNodes(List<String> sourceQuestionIds, String targetUserId, String userId) {
        // 1. 권한 체크
        for (String srcId : sourceQuestionIds) {
            nodeUtilService.checkOwnership(srcId, userId);
        }

        // 2. 상대 ID에 대한 유효성 검사와 상대 UserNode 가져옴
        UserNode targetUser = userNodeService.getUserByEmailId(targetUserId);

        // 3. 새로 생성될 토픽의 이름은 최상위 질문의 Text
        QuestionNode rootQuestion = questionRepository.findById(sourceQuestionIds.get(0))
                .orElseThrow(() -> new ResourceNotFoundException("질문을 찾을 수 없습니다."));
        String newTopicName = rootQuestion.getText();

        // 4. 상대의 새로운 토픽 생성.
        TopicNode newTopic = TopicNode.createTopic(newTopicName, targetUser);
        topicRepository.save(newTopic);

        // 5. 서브트리 상대 토픽에 복제
        List<String> newQuestionIds = questionRepository.copyPartialQuestionTree(sourceQuestionIds,
                newTopic.getTopicId());
    }

    public void favoriteQuestionNode(String questionId, String userId) {
        // 권한 체크
        nodeUtilService.checkOwnership(questionId, userId);
        QuestionNode question = questionRepository.findById(questionId).orElseThrow();
        // 원래의 isFavorite 속성의 반대 값으로 설정
        question.setFavorite(!question.isFavorite());
        questionRepository.save(question);
    }

    private List<QuestionAnswerDTO> findQuestionByKeyword(String keyword, String userId) {
        return questionRepository.findQuestionAndAnswerByKeyword(keyword, userId);
    }

    private Map<String, List<QuestionAnswerDTO>> groupBasedTopicId(List<QuestionAnswerDTO> questions) {
        Map<String, List<QuestionAnswerDTO>> groupedByTopic = new HashMap<>();

        for (QuestionAnswerDTO dto : questions) {
            String questionId = dto.getQuestionId();

            String topicId = topicRepository.findTopicIdByQuestionId(questionId)
                    .orElseThrow(() -> new ResourceNotFoundException("토픽이 존재하지 않습니다."));

            // topicId 기준으로 리스트 분류
            groupedByTopic
                    .computeIfAbsent(topicId, k -> new ArrayList<>())
                    .add(dto);
        }

        return groupedByTopic;
    }

    private List<TopicTreeMapResponseDTO> convertTopicTreeMapResult(
            Map<String, List<QuestionAnswerDTO>> groupedByTopic) {
        List<TopicTreeMapResponseDTO> result = new ArrayList<>();

        for (Map.Entry<String, List<QuestionAnswerDTO>> entry : groupedByTopic.entrySet()) {
            String topicId = entry.getKey();
            List<QuestionAnswerDTO> flatList = entry.getValue();

            TopicTreeMapResponseDTO topicTree = nodeUtilService.buildMapFromFlatList(flatList, topicId, false);
            result.add(topicTree);
        }

        return result;
    }
}
