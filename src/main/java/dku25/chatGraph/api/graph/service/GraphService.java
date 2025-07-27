package dku25.chatGraph.api.graph.service;

import dku25.chatGraph.api.graph.dto.QuestionTreeNodeDTO;
import dku25.chatGraph.api.graph.node.AnswerNode;
import dku25.chatGraph.api.graph.node.QuestionNode;
import dku25.chatGraph.api.graph.node.TopicNode;
import dku25.chatGraph.api.graph.node.UserNode;
import dku25.chatGraph.api.graph.repository.AnswerRepository;
import dku25.chatGraph.api.graph.repository.QuestionRepository;
import dku25.chatGraph.api.graph.repository.TopicRepository;
import dku25.chatGraph.api.graph.repository.UserNodeRepository;
import dku25.chatGraph.api.graph.dto.QuestionAnswerDTO;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class GraphService {

    private final TopicRepository topicRepository;
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final UserNodeRepository userNodeRepository;

    private final UserNodeService userNodeService;
    private final QuestionService questionService;
    private final TopicService topicService;
    private final AnswerService answerService;

    @Autowired
    public GraphService(TopicRepository topicRepository, QuestionRepository questionRepository, AnswerRepository answerRepository, UserNodeRepository userNodeRepository,
                        UserNodeService userNodeService, QuestionService questionService, TopicService topicService, AnswerService answerService) {
        this.topicRepository = topicRepository;
        this.questionRepository = questionRepository;
        this.answerRepository = answerRepository;
        this.userNodeRepository = userNodeRepository;
        this.userNodeService = userNodeService;
        this.questionService = questionService;
        this.topicService = topicService;
        this.answerService = answerService;
    }

    /**
     * 질문, 답변 저장 로직
     */
    public QuestionNode saveQuestionAndAnswer(String prompt, String userId, String answer, String previousQuestionId) {
        if (previousQuestionId == null) {
            UserNode user = userNodeService.getUserById(userId);
            QuestionNode rootQuestion = createQuestionWithAnswer(prompt, answer, null);
            topicService.createTopicForFirstQuestion("New Chat", user, rootQuestion);
            return rootQuestion;
        } else if (topicRepository.existsById(previousQuestionId)) {
            TopicNode topic = topicService.findTopicNodeById(previousQuestionId).orElseThrow();
            QuestionNode rootQuestion = createQuestionWithAnswer(prompt, answer, null);
            topicService.linkFirstQuestionToTopic(topic, rootQuestion);
            return rootQuestion;
        } else {
            QuestionNode previousQuestion = questionService.findQuestionNodeById(previousQuestionId).orElseThrow();
            QuestionNode currentQuestion = createQuestionWithAnswer(prompt, answer, previousQuestion);
            questionService.linkFollowedByToQuestion(previousQuestion, currentQuestion);
            return currentQuestion;
        }
    }

    /**
     * 질문 및 답변 생성 및 관계 설정
     */
    @NotNull
    private QuestionNode createQuestionWithAnswer(String prompt, String answer, QuestionNode previousQuestion) {
        AnswerNode answerNode = answerService.createAndSaveAnswer(answer);
        QuestionNode questionNode = questionService.createQuestionNode(prompt, previousQuestion);
        questionNode.setAnswer(answerNode);
        return questionNode;
    }

    public List<QuestionTreeNodeDTO> buildTreeFromFlatList(List<QuestionAnswerDTO> flatList, String topicId) {
        // 1. 모든 노드를 Map에 questionId 기준으로 저장
        Map<String, QuestionTreeNodeDTO> nodeMap = new HashMap<>();
        for (QuestionAnswerDTO dto : flatList) {
            nodeMap.put(dto.getQuestionId(),
                new QuestionTreeNodeDTO(
                        dto.getQuestionId(),
                        dto.getQuestion(),
                        dto.getLevel(),
                        dto.getAnswerId(),
                        dto.getAnswer(),
                        dto.getCreatedAt(),
                        new ArrayList<>()
                )
            );
        }

        // 2. 부모-자식 연결
        List<QuestionTreeNodeDTO> roots = new ArrayList<>();
        for (QuestionAnswerDTO dto : flatList) {
            QuestionTreeNodeDTO current = nodeMap.get(dto.getQuestionId());
            if (dto.getParentId() == null || dto.getParentId().equals(topicId)) {
                // 토픽이 root인 경우 topicId와 연결 or parentId==null이면 루트
                roots.add(current);
            } else {
                QuestionTreeNodeDTO parent = nodeMap.get(dto.getParentId());
                if (parent != null) parent.getChildren().add(current);
            }
        }
        return roots; // 최상위 루트들 반환
    }
}
