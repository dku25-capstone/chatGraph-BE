package dku25.chatGraph.api.graph.service;

import dku25.chatGraph.api.graph.node.AnswerNode;
import dku25.chatGraph.api.graph.node.QuestionNode;
import dku25.chatGraph.api.graph.node.TopicNode;
import dku25.chatGraph.api.graph.repository.AnswerRepository;
import dku25.chatGraph.api.graph.repository.QuestionRepository;
import dku25.chatGraph.api.graph.repository.TopicRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import dku25.chatGraph.api.graph.repository.UserGraphRepository;
import dku25.chatGraph.api.graph.node.UserNode;

import java.util.List;
import java.util.Optional;

@Service
public class GraphService {

    private final TopicRepository topicRepository;
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final UserGraphRepository userGraphRepository;

    @Autowired
    public GraphService(TopicRepository topicRepository, QuestionRepository questionRepository, AnswerRepository answerRepository, UserGraphRepository userGraphRepository) {
        this.topicRepository = topicRepository;
        this.questionRepository = questionRepository;
        this.answerRepository = answerRepository;
        this.userGraphRepository = userGraphRepository;
    }

    public QuestionNode saveQuestionAndAnswer(String prompt, String userId, String answer, String previousQuestionId) {
        QuestionNode prevQuestionNode = (previousQuestionId != null && !previousQuestionId.isEmpty())
                ? questionRepository.findById(previousQuestionId).orElse(null)
                : null;

        AnswerNode answerNode = AnswerNode.createAnswer(answer);
        answerRepository.save(answerNode);

        QuestionNode currentQuestionNode = QuestionNode.createQuestion(prompt, prevQuestionNode);
        currentQuestionNode.setAnswer(answerNode);

        if (prevQuestionNode != null) {
            prevQuestionNode.setFollowedBy(currentQuestionNode);
            questionRepository.save(prevQuestionNode);
        } else {
            UserNode userNode = userGraphRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 사용자입니다.")); // userId를 통한 usernode 생성
            TopicNode topicNode = TopicNode.createTopic("New Chat", userNode);
            topicNode.setFirstQuestion(currentQuestionNode);
            topicRepository.save(topicNode);
        }

        questionRepository.save(currentQuestionNode);

        return currentQuestionNode;
    }

    public Optional<QuestionNode> findQuestionById(String id) {
        return questionRepository.findById(id);
    }

    // public List<TopicNode> findAllTopicsBySessionId(String sessionId) {
    //     return topicRepository.findBySessionId(sessionId);
    // }

    /**
     * 회원가입 시 호출: userId로 Neo4j에 UserNode 생성
     */
    public void createUserNode(String userId) {
        userGraphRepository.save(new UserNode(userId));
        System.out.println("Neo4j 유저노드 생성 성공: " + userId);
    }
}
