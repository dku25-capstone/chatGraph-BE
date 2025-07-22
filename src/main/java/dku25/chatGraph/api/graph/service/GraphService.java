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
import org.springframework.transaction.annotation.Transactional;

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
        System.out.println("userId = " + userId);
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
        checkTopicOwnership(topicId, userId);
        return topicRepository.findQuestionsAndAnswersByTopicId(topicId);
    }

    // 토픽명 수정 -> TopicNode에 들어갈 것
    public TopicResponseDTO renameTopic(String topicId, String userId, String newTopicName) {
        checkTopicOwnership(topicId, userId);
        return topicRepository.renameTopic(topicId, newTopicName);
    }

    // 토픽 삭제
    public void deleteTopic(String topicId, String userId) {
        checkTopicOwnership(topicId, userId);
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
        QuestionNode parentQuestion = questionRepository.getPreviousQuestion(toDelete.getQuestionId());
        TopicNode parentTopic = null;
        if (parentQuestion == null) {
            // 상위 질문노드가 없으면, 토픽이 부모
            parentTopic = topicRepository.findTopicByFirstQuestionId(questionId).orElseThrow(() -> new RuntimeException("토픽 노드 없음"));
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
                for (QuestionNode child : childQuestions) {
                    // 1. 자식들을 부모의 followedBy에 모두 추가
                    questionRepository.createFollowedByRelation(parentQuestion.getQuestionId(), child.getQuestionId());
                    // 2. 각 자식의 previousQuestion을 부모로 설정
                    //child.setPreviousQuestion(parentQuestion);
                    // 테스트 시 레벨 모두 정상
                    System.out.printf("[LOG] childId=%s, level=%d, prevId=%s%n",
                            child.getQuestionId(),
                            child.getLevel(),
                            questionRepository.getPreviousQuestion(child.getQuestionId()) != null ? questionRepository.getPreviousQuestion(child.getQuestionId()).getQuestionId() : "null"
                    );
                    //save후 DB에서의 level도 정상
                    questionRepository.save(child);
                    QuestionNode check = questionRepository.findById(child.getQuestionId()).get();
                    System.out.println("DB level: " + check.getLevel());
                }
                // questionRepository.save(parentQuestion);
            } else if (parentTopic != null) {
                for (QuestionNode child : childQuestions) {
                    topicRepository.createStartConversationRelation(parentTopic.getTopicId(), child.getQuestionId());
                    //child.setPreviousQuestion(null); // previousQuestion 제거
                    questionRepository.save(child);
                }
            }
        }

        // 답변노드 삭제
        AnswerNode answer = toDelete.getAnswer();
        if (answer != null) {
            answerRepository.delete(answer);
        }

        // 질문노드 삭제
        questionRepository.delete(toDelete);
    }

    private void checkTopicOwnership(String topicId, String userId) {
        // 사용자가 해당 토픽의 소유자인지 확인
        TopicNode topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new IllegalArgumentException("토픽을 찾을 수 없습니다."));

        if (topic.getUser() == null || !topic.getUser().getUserId().equals(userId)) {
            throw new IllegalArgumentException("해당 토픽에 대한 접근 권한이 없습니다.");
        }
    }
}
