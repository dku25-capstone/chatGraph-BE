package dku25.chatGraph.api.graph.service;

import dku25.chatGraph.api.graph.dto.RenameQuestionResponseDTO;
import dku25.chatGraph.api.graph.dto.TopicTreeMapResponseDTO;
import dku25.chatGraph.api.graph.dto.QuestionAnswerDTO;
import dku25.chatGraph.api.graph.node.QuestionNode;
import dku25.chatGraph.api.graph.repository.QuestionRepository;
import dku25.chatGraph.api.graph.repository.TopicRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class QuestionService {
    private final QuestionRepository questionRepository;
    private final TopicRepository topicRepository;
    private final NodeUtilService nodeUtilService;

    @Autowired
    public QuestionService(QuestionRepository questionRepository, TopicRepository topicRepository, NodeUtilService nodeUtilService) {
        this.questionRepository = questionRepository;
        this.topicRepository = topicRepository;
        this.nodeUtilService = nodeUtilService;
    }

    // Query Parameter로 QuestionNode 조회
    public List<TopicTreeMapResponseDTO> searchByKeyword(String keyword, String userId) {
       // 1. 키워드 기반 질문 + 답변 조회
            List<QuestionAnswerDTO> questionList = questionRepository.findQuestionAndAnswerByKeyword(keyword, userId);

        // 2. topicId 기준으로 그룹화
        Map<String, List<QuestionAnswerDTO>> groupedByTopic = new HashMap<>();

        for (QuestionAnswerDTO dto : questionList) {
            String questionId = dto.getQuestionId();

            // 3. 각 질문에 대한 topicId 조회
            String topicId = topicRepository.findTopicIdByQuestionId(questionId).orElseThrow(() -> new RuntimeException("토픽 ID 조회 실패")); // 아래 @Query 참조

            // 4. topicId 기준으로 리스트 분류
            groupedByTopic
                .computeIfAbsent(topicId, k -> new ArrayList<>())
                .add(dto);
        }

        // 5. 결과 변환
        List<TopicTreeMapResponseDTO> result = new ArrayList<>();
        for (Map.Entry<String, List<QuestionAnswerDTO>> entry : groupedByTopic.entrySet()) {
            String topicId = entry.getKey();
            List<QuestionAnswerDTO> flatList = entry.getValue();

            TopicTreeMapResponseDTO topicTree = nodeUtilService.buildMapFromFlatList(flatList, topicId, false);
            result.add(topicTree);
        }

        return result;
    }

    // QuestionNode 생성
    public QuestionNode createQuestionNode(String prompt, QuestionNode previousQuestion){
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
            System.out.println("questionId = " + questionId);
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
}
