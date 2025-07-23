package dku25.chatGraph.api.graph.service;

import dku25.chatGraph.api.graph.node.AnswerNode;
import dku25.chatGraph.api.graph.node.QuestionNode;
import dku25.chatGraph.api.graph.node.TopicNode;
import dku25.chatGraph.api.graph.node.UserNode;
import dku25.chatGraph.api.graph.repository.AnswerRepository;
import dku25.chatGraph.api.graph.repository.QuestionRepository;
import dku25.chatGraph.api.graph.repository.TopicRepository;
import dku25.chatGraph.api.graph.repository.UserNodeRepository;
import dku25.chatGraph.api.graph.dto.TopicResponseDTO;
import dku25.chatGraph.api.graph.dto.QuestionAnswerDTO;
import dku25.chatGraph.api.graph.dto.QuestionResponseDTO;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
}
