package dku25.chatGraph.api.graph.service;

import dku25.chatGraph.api.graph.node.AnswerNode;
import dku25.chatGraph.api.graph.node.QuestionNode;
import dku25.chatGraph.api.graph.repository.AnswerRepository;
import dku25.chatGraph.api.graph.repository.QuestionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class GraphService {

    private final AnswerRepository answerRepository;

    private final QuestionRepository questionRepository;

    @Autowired
    public GraphService(AnswerRepository answerRepository, QuestionRepository questionRepository) {
        this.answerRepository = answerRepository;
        this.questionRepository = questionRepository;
    }

    public void saveToNeo4j(String sessionId, String prompt, String answer) {
        AnswerNode answerNode = new AnswerNode();
        answerNode.setText(answer);
        answerRepository.save(answerNode);

        QuestionNode questionNode = new QuestionNode();
        questionNode.setText(prompt);
        questionNode.setSessionId(sessionId);
        questionNode.setAnswer(answerNode);
        questionRepository.save(questionNode);
    }
}


