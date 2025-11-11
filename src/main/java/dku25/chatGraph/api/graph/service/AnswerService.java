package dku25.chatGraph.api.graph.service;

import dku25.chatGraph.api.graph.node.AnswerNode;
import dku25.chatGraph.api.graph.repository.AnswerRepository;
import org.springframework.stereotype.Service;

@Service
public class AnswerService {
    private final AnswerRepository answerRepository;

    public AnswerService(AnswerRepository answerRepository) {
        this.answerRepository = answerRepository;
    }

    // Answer노드 생성 및 저장
    public AnswerNode createAndSaveAnswer(String answer) {
        AnswerNode answerNode = AnswerNode.createAnswer(answer);
        return answerRepository.save(answerNode);
    }
}
