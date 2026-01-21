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

        checkQuestions(sourceQuestionIds, userId);

        return questionRepository.copyPartialQuestionTree(sourceQuestionIds, targetParentId);
    }

    // 서브트리를 새로운 토픽으로 이동 (복제 + 원본 삭제)
    @Transactional
    public MoveToNewTopicResponseDTO moveToNewTopic(List<String> sourceQuestionIds, String userId) {
        checkQuestions(sourceQuestionIds, userId);

        QuestionNode rootQuestion = questionRepository.findById(sourceQuestionIds.getFirst())
                .orElseThrow(() -> new ResourceNotFoundException("질문을 찾을 수 없습니다."));

        UserNode user = userNodeService.getUserById(userId);

        String newTopicName = rootQuestion.getText();
        TopicNode newTopic = createNewTopic(newTopicName, user);

        List<String> newQuestionIds = questionRepository.copyPartialQuestionTree(sourceQuestionIds,
                newTopic.getTopicId());

        return MoveToNewTopicResponseDTO.builder()
                .newTopicId(newTopic.getTopicId())
                .newQuestionIds(newQuestionIds)
                .build();
    }

    @Transactional
    public void shareQuestionNodes(List<String> sourceQuestionIds, String targetUserId, String userId) {
        checkQuestions(sourceQuestionIds, userId);

        QuestionNode rootQuestion = questionRepository.findById(sourceQuestionIds.getFirst())
                .orElseThrow(() -> new ResourceNotFoundException("질문을 찾을 수 없습니다."));

        UserNode targetUser = userNodeService.getUserByEmailId(targetUserId);

        String newTopicName = rootQuestion.getText();
        TopicNode newTopic = createNewTopic(newTopicName, targetUser);

        questionRepository.copyPartialQuestionTree(sourceQuestionIds,
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

    private void checkQuestions(List<String> sourceQuestionIds, String userId) {
        for (String srcId : sourceQuestionIds) {
            nodeUtilService.checkOwnership(srcId, userId);
        }
    }

    private TopicNode createNewTopic(String topicName, UserNode user) {
        TopicNode newTopic = TopicNode.createTopic(topicName, user);
        topicRepository.save(newTopic);

        return newTopic;
    }
}
