package dku25.chatGraph.api.graph.service;

import dku25.chatGraph.api.graph.node.AnswerNode;
import dku25.chatGraph.api.graph.node.QuestionNode;
import dku25.chatGraph.api.graph.node.TopicNode;
import dku25.chatGraph.api.graph.node.UserNode;
import dku25.chatGraph.api.graph.repository.AnswerRepository;
import dku25.chatGraph.api.graph.repository.QuestionRepository;
import dku25.chatGraph.api.graph.repository.TopicRepository;
import dku25.chatGraph.api.graph.repository.UserGraphRepository;
import dku25.chatGraph.api.graph.dto.TopicResponseDTO;
import dku25.chatGraph.api.graph.dto.QuestionAnswerDTO;
import dku25.chatGraph.api.graph.dto.QuestionResponseDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.ArrayList;

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
        QuestionNode prevQuestionNode = null;
        TopicNode topicNode = null;
        boolean isTopicRoot = false;

        if (previousQuestionId != null && !previousQuestionId.isEmpty()) {
            // 토픽 ID로 먼저 조회
            Optional<TopicNode> topicOpt = topicRepository.findById(previousQuestionId);
            if (topicOpt.isPresent()) {
                topicNode = topicOpt.get();
                isTopicRoot = true;
            } else {
                // 질문 ID로 조회
                prevQuestionNode = questionRepository.findById(previousQuestionId).orElse(null);
            }
        }

        AnswerNode answerNode = AnswerNode.createAnswer(answer);
        answerRepository.save(answerNode);

        QuestionNode currentQuestionNode = QuestionNode.createQuestion(prompt, prevQuestionNode);
        currentQuestionNode.setAnswer(answerNode);

        if (prevQuestionNode != null) {
            prevQuestionNode.setFollowedBy(currentQuestionNode);
            questionRepository.save(prevQuestionNode);
        }

        // 토픽 루트 질문 처리
        if (isTopicRoot) {
            topicNode.setFirstQuestion(currentQuestionNode);
            topicRepository.save(topicNode);
        } else if (previousQuestionId == null) {
            UserNode userNode = userGraphRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 사용자입니다.")); // userId를 통한 usernode 생성
            TopicNode newTopicNode = TopicNode.createTopic("New Chat", userNode);
            newTopicNode.setFirstQuestion(currentQuestionNode);
            topicRepository.save(newTopicNode);
        }

        questionRepository.save(currentQuestionNode);
        return currentQuestionNode;
    }

    public Optional<QuestionNode> findQuestionById(String id) {
        return questionRepository.findById(id);
    }

    /**
     * 회원가입 시 호출: userId로 Neo4j에 UserNode 생성
     */
    public void createUserNode(String userId) {
        userGraphRepository.save(UserNode.createUser(userId));
        System.out.println("Neo4j 유저노드 생성 성공: " + userId);
    }

    /**
     * 사용자의 토픽 목록 조회
     */
    public List<TopicResponseDTO> getTopicsByUserId(String userId) {
        UserNode user = userGraphRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 사용자입니다."));

        if (user.getTopics() == null) {
            return List.of();
        }

        return user.getTopics().stream()
                .map(topic -> TopicResponseDTO.builder()
                        .topicId(topic.getTopicId())
                        .topicName(topic.getTopicName())
                        .createdAt(topic.getCreatedAt() != null ? topic.getCreatedAt() : null)
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 토픽의 질문-답변 목록 조회
     */
    public List<QuestionAnswerDTO> getTopicQuestionsAndAnswers(String topicId, String userId) {
        // 사용자가 해당 토픽의 소유자인지 확인
        TopicNode topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new IllegalArgumentException("토픽을 찾을 수 없습니다."));

        if (topic.getUser() == null || !topic.getUser().getUserId().equals(userId)) {
            throw new IllegalArgumentException("해당 토픽에 대한 접근 권한이 없습니다.");
        }

        // DTO로 바로 반환
        return topicRepository.findQuestionsAndAnswersByTopicId(topicId);
    }

    // 토픽명 수정
    public TopicResponseDTO renameTopic(String topicId, String userId,String newTopicName) {
        // 사용자가 해당 토픽의 소유자인지 확인
        TopicNode topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new IllegalArgumentException("토픽을 찾을 수 없습니다."));

        if (topic.getUser() == null || !topic.getUser().getUserId().equals(userId)) {
            throw new IllegalArgumentException("해당 토픽에 대한 접근 권한이 없습니다.");
        }

        return topicRepository.renameTopic(topicId, newTopicName);
    }

    public void deleteTopic(String topicId, String userId) {
        // 사용자가 해당 토픽의 소유자인지 확인
        TopicNode topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new IllegalArgumentException("토픽을 찾을 수 없습니다."));

        if (topic.getUser() == null || !topic.getUser().getUserId().equals(userId)) {
            throw new IllegalArgumentException("해당 토픽에 대한 접근 권한이 없습니다.");
        }

        topicRepository.deleteById(topicId);
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
        //삭제할 질문노드 조회
        QuestionNode toDelete = questionRepository.findById(questionId)
            .orElseThrow(() -> new RuntimeException("질문 노드 없음"));

        //부모 노드(상위 노드) 조회 및 판별
        QuestionNode parentQuestion = toDelete.getPreviousQuestion();
        TopicNode parentTopic = null;
        if (parentQuestion == null) {
            // 상위 질문노드가 없으면, 토픽이 부모
            // 실제 필드명에 맞게 getTopic() 등으로 수정 필요
            // parentTopic = toDelete.getTopic();
        }
        // parentQuestion 또는 parentTopic 중 하나가 부모

        // 자식(하위) 노드 조회
        List<QuestionNode> childQuestions = questionRepository.findChildrenByParentId(questionId);
        if (childQuestions != null && !childQuestions.isEmpty()) {
            // 모든 자식에 대해 처리
        }

        // 관계 재설정
        if (!childQuestions.isEmpty()) {
            if (parentQuestion != null) {
                // 1. 삭제할 노드를 부모의 followedBy에서 제거
                questionRepository.removeFollowedByRelation(parentQuestion.getQuestionId(), questionId);
               
                for (QuestionNode child : childQuestions) {
                     // 2. 자식들을 부모의 followedBy에 모두 추가
                    questionRepository.createFollowedByRelation(parentQuestion.getQuestionId(), child.getQuestionId());
                     // 3. 각 자식의 previousQuestion을 부모로 설정
                    child.setPreviousQuestion(parentQuestion);
                    questionRepository.save(child);
                }
                questionRepository.save(parentQuestion);
            } else if (parentTopic != null) {
                // (토픽-자식 연결도 필요하다면 여기에 List 기반으로 구현)
            }
        }

        //자식 노드들의 level 1씩 감소
        for (QuestionNode child : childQuestions) {;
            updateLevelRecursively(child, child.getLevel()); // Q3의 자식들부터 재귀적으로 level 맞춤
        }

        // 답변노드 삭제
        AnswerNode answer = toDelete.getAnswer();
        if (answer != null) {
            answerRepository.delete(answer);
        }

        // 질문노드 삭제
        questionRepository.delete(toDelete);
    }

    // 하위 노드들의 level을 재귀적으로 변경하는 메서드
    private void updateLevelRecursively(QuestionNode node, int parentLevel) {
        List<QuestionNode> childQuestions = questionRepository.findChildrenByParentId(node.getQuestionId());
        if(childQuestions !=null ){
            for (QuestionNode child : childQuestions) {
                child.setLevel(parentLevel + 1);
                questionRepository.save(child);
                updateLevelRecursively(child, child.getLevel());
        }
        }
        
    }
}
