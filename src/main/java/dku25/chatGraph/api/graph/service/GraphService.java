package dku25.chatGraph.api.graph.service;

import dku25.chatGraph.api.graph.node.AnswerNode;
import dku25.chatGraph.api.graph.node.QuestionNode;
import dku25.chatGraph.api.graph.node.TopicNode;
import dku25.chatGraph.api.graph.repository.AnswerRepository;
import dku25.chatGraph.api.graph.repository.QuestionRepository;
import dku25.chatGraph.api.graph.repository.TopicRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

    public void saveFirstQuestion(String prompt, String sessionId, String answer) {
        QuestionNode firstQuestion = createAndSaveQuestion(prompt, answer, null);

        TopicNode topic = TopicNode.createTopic("New Chat", sessionId);
        topic.setFirstQuestion(firstQuestion);
        topicRepository.save(topic);
    }

    public void saveFollowUpQuestion(String prompt, String sessionId, String answer, String previousQuestionId) {
        QuestionNode previousQuestion = questionRepository.findById(previousQuestionId).orElseThrow(() -> new IllegalArgumentException("이전 질문이 없습니다."));
        createAndSaveQuestion(prompt, answer, previousQuestion);
    }

    public Optional<QuestionNode> findQuestionById(String id){
        return questionRepository.findById(id);
    }

    private QuestionNode createAndSaveQuestion(String prompt, String answer, QuestionNode previousQuestion) {
        AnswerNode answerNode = AnswerNode.createAnswer(answer);
        answerRepository.save(answerNode);

        QuestionNode currentQuestion = QuestionNode.createQuestion(prompt, previousQuestion);
        currentQuestion.setAnswer(answerNode);

        if (previousQuestion != null) {
            previousQuestion.setFollowedBy(currentQuestion);
            questionRepository.save(previousQuestion);
        }

        return questionRepository.save(currentQuestion);
    }
}


