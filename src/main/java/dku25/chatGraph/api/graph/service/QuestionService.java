package dku25.chatGraph.api.graph.service;

import dku25.chatGraph.api.graph.dto.RenameQuestionResponseDTO;
import dku25.chatGraph.api.graph.dto.QuestionAnswerDTO;
import dku25.chatGraph.api.graph.node.QuestionNode;
import dku25.chatGraph.api.graph.repository.QuestionRepository;
import dku25.chatGraph.api.graph.repository.TopicRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

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
    public List<QuestionAnswerDTO> searchByKeyword(String keyword, String userId) {
        return questionRepository.findQuestionAndAnswerByKeyword(keyword, userId);
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
