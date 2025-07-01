package dku25.chatGraph.api.graph.service;

import dku25.chatGraph.api.graph.node.AnswerNode;
import dku25.chatGraph.api.graph.node.QuestionNode;
import dku25.chatGraph.api.graph.repository.AnswerRepository;
import dku25.chatGraph.api.graph.repository.QuestionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class GraphService {

    private final AnswerRepository answerRepository;
    private final QuestionRepository questionRepository;

    @Autowired
    public GraphService(AnswerRepository answerRepository, QuestionRepository questionRepository) {
        this.answerRepository = answerRepository;
        this.questionRepository = questionRepository;
    }

    public void saveToNeo4j(String sessionId, String prompt, String answer, Optional<QuestionNode> previousQuestionOpt) {
        AnswerNode answerNode = AnswerNode.create(answer);
        answerRepository.save(answerNode);

        QuestionNode previousQuestion = previousQuestionOpt.orElse(null);
        QuestionNode currentQuestion = QuestionNode.create(prompt, sessionId, previousQuestion);
        currentQuestion.setAnswer(answerNode);

        if (previousQuestion != null) {
            previousQuestion.setFollowedBy(currentQuestion);
            previousQuestion.setPreviousQuestion(currentQuestion);
        }

        questionRepository.save(currentQuestion);
    }

    public void saveToNeo4j(String sessionId, String prompt, String answer) {
        saveToNeo4j(sessionId, prompt, answer, Optional.empty());
    }

    public Optional<QuestionNode> findQuestionById(String id){
        return questionRepository.findById(id);
    }
}


