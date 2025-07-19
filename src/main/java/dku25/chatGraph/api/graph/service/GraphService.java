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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
}
