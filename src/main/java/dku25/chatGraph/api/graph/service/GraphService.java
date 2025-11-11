package dku25.chatGraph.api.graph.service;

import dku25.chatGraph.api.graph.dto.NodeRenameResponseDTO;
import dku25.chatGraph.api.graph.dto.RenameQuestionResponseDTO;
import dku25.chatGraph.api.graph.dto.RenameTopicResponseDTO;
import dku25.chatGraph.api.graph.node.AnswerNode;
import dku25.chatGraph.api.graph.node.QuestionNode;
import dku25.chatGraph.api.graph.node.TopicNode;
import dku25.chatGraph.api.graph.node.UserNode;
import dku25.chatGraph.api.graph.repository.AnswerRepository;
import dku25.chatGraph.api.graph.repository.QuestionRepository;
import dku25.chatGraph.api.graph.repository.TopicRepository;
import dku25.chatGraph.api.graph.repository.UserNodeRepository;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;

@Service
public class GraphService {

    private final TopicRepository topicRepository;
    private final QuestionRepository questionRepository;

    private final UserNodeService userNodeService;
    private final QuestionService questionService;
    private final TopicService topicService;
    private final AnswerService answerService;
    private final NodeUtilService nodeUtilService;

    public GraphService(TopicRepository topicRepository, QuestionRepository questionRepository, AnswerRepository answerRepository, UserNodeRepository userNodeRepository,
                        UserNodeService userNodeService, QuestionService questionService, TopicService topicService, AnswerService answerService, NodeUtilService nodeUtilService) {
        this.topicRepository = topicRepository;
        this.questionRepository = questionRepository;
        this.userNodeService = userNodeService;
        this.questionService = questionService;
        this.topicService = topicService;
        this.answerService = answerService;
        this.nodeUtilService = nodeUtilService;
    }

    /**
     * 질문, 답변 저장 로직
     */
    public QuestionNode saveQuestionAndAnswer(String prompt, String userId, String answer, String previousQuestionId, @Nullable String topicSummary) {
        if (previousQuestionId == null) {
            UserNode user = userNodeService.getUserById(userId);
            QuestionNode rootQuestion = createQuestionWithAnswer(prompt, answer, null);
            topicService.createTopicForFirstQuestion(topicSummary, user, rootQuestion);
            return rootQuestion;
        } else if (topicRepository.existsById(previousQuestionId)) {
            TopicNode topic = topicService.findTopicNodeById(previousQuestionId).orElseThrow();
            QuestionNode rootQuestion = createQuestionWithAnswer(prompt, answer, null);
            topicService.linkFirstQuestionToTopic(topic, rootQuestion);
            return rootQuestion;
        } else {
            QuestionNode previousQuestion = questionRepository.findById(previousQuestionId).orElseThrow();
            QuestionNode currentQuestion = createQuestionWithAnswer(prompt, answer, previousQuestion);
            questionService.linkFollowedByToQuestion(previousQuestion, currentQuestion);
            return currentQuestion;
        }
    }

    // 노드(질문, 토픽)명 수정
    public NodeRenameResponseDTO renameNode(String nodeId, String userId, String newName) {
        nodeUtilService.checkOwnership(nodeId, userId);
        if (nodeId.startsWith("topic-")) {
            RenameTopicResponseDTO dto = topicRepository.renameTopic(nodeId, newName);
            return new NodeRenameResponseDTO(nodeId, "topic", dto);
        } else if (nodeId.startsWith("question-")) {
            RenameQuestionResponseDTO dto = questionRepository.renameQuestion(nodeId, newName);
            return new NodeRenameResponseDTO(nodeId, "question", dto);
        }
        throw new IllegalArgumentException("지원하지 않는 node입니다. " + nodeId);
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
}
