package dku25.chatGraph.api.graph.service;

import dku25.chatGraph.api.graph.node.AnswerNode;
import dku25.chatGraph.api.graph.node.QuestionNode;
import dku25.chatGraph.api.graph.node.TopicNode;
import dku25.chatGraph.api.graph.repository.AnswerRepository;
import dku25.chatGraph.api.graph.repository.QuestionRepository;
import dku25.chatGraph.api.graph.repository.TopicRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class GraphService {

    private final TopicRepository topicRepository;
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;

    @Autowired
    public GraphService(TopicRepository topicRepository, QuestionRepository questionRepository, AnswerRepository answerRepository) {
        this.topicRepository = topicRepository;
        this.questionRepository = questionRepository;
        this.answerRepository = answerRepository;
    }

    public void saveQuestionAndAnswer(String prompt, String sessionId, String answer, String previousQuestionId) {
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
            TopicNode topicNode = TopicNode.createTopic("New Chat", sessionId);
            topicNode.setFirstQuestion(currentQuestionNode);
            topicRepository.save(topicNode);
        }

        questionRepository.save(currentQuestionNode);
    }

    public Optional<QuestionNode> findQuestionById(String id) {
        return questionRepository.findById(id);
    }

    public List<TopicNode> findAllTopicsBySessionId(String sessionId) {
        return topicRepository.findBySessionId(sessionId);
    }
}
