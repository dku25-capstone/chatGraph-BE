package dku25.chatGraph.api.graph.service;

import dku25.chatGraph.api.graph.dto.QuestionResponseDTO;
import dku25.chatGraph.api.graph.node.QuestionNode;
import dku25.chatGraph.api.graph.repository.QuestionRepository;
import dku25.chatGraph.api.graph.repository.TopicRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class QuestionService {
    private final QuestionRepository questionRepository;
    private final TopicRepository topicRepository;

    @Autowired
    public QuestionService(QuestionRepository questionRepository, TopicRepository topicRepository) {
        this.questionRepository = questionRepository;
        this.topicRepository = topicRepository;
    }

    // QuestionId로 QuestionNode 조회
    public Optional<QuestionNode> findQuestionNodeById(String id) {
        return questionRepository.findById(id);
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

    // 질문 노드 질문명 수정
    public QuestionResponseDTO renameQuestion(String questionId, String newQuestionName) {
        QuestionNode question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("질문 노드 없음"));
        question.setText(newQuestionName);
        questionRepository.save(question);
        QuestionResponseDTO dto = new QuestionResponseDTO();
        dto.setQuestionId(question.getQuestionId());
        dto.setText(question.getText());
        return dto;
    }

    // 질문 노드 삭제 -> 이에 따른 답변 노드도 삭제
    public void deleteQuestionNode(String questionId) {
        // 상위 노드가 질문 노드인지, 토픽 노드인지 판별
        QuestionNode parentQuestion = questionRepository.getPreviousQuestion(questionId);

        if (parentQuestion != null) {
            // 상위 노드가 질문일 경우
            questionRepository.deleteAndRelink(questionId);
        } else {
            // 상위 노드가 토픽일 경우 (첫 질문)
            topicRepository.deleteFirstQuestionAndRelink(questionId);
        }
    }
}
