package dku25.chatGraph.api.graph.service;

import dku25.chatGraph.api.graph.dto.*;
import dku25.chatGraph.api.graph.node.QuestionNode;
import dku25.chatGraph.api.graph.node.TopicNode;
import dku25.chatGraph.api.graph.node.UserNode;
import dku25.chatGraph.api.graph.repository.TopicRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class TopicService {
    private final TopicRepository topicRepository;
    private final UserNodeService userNodeService;
    private final NodeUtilService nodeUtilService;

    @Autowired
    public TopicService(TopicRepository topicRepository, UserNodeService userNodeService, NodeUtilService nodeUtilService) {
        this.topicRepository = topicRepository;
        this.userNodeService = userNodeService;
        this.nodeUtilService = nodeUtilService;
    }

    // Topic ID로 토픽 조회
    public Optional<TopicNode> findTopicNodeById(String id) {
        return topicRepository.findById(id);
    }

    // 첫 질문일때 토픽 생성 및 관계 생성
    public void createTopicForFirstQuestion(String topicName, UserNode user, QuestionNode firstQuestion) {
        TopicNode topic = TopicNode.createTopic(topicName, user);
        linkFirstQuestionToTopic(topic, firstQuestion);
    }

    // 토픽에 대한 1계층 질문일때 관계 생성
    public void linkFirstQuestionToTopic(TopicNode topic, QuestionNode question) {
        topic.setFirstQuestion(question);
        topicRepository.save(topic);
    }

    // 사용자의 토픽 목록 조회
    public List<RenameTopicResponseDTO> getTopicsByUserId(String userId) {
        UserNode user = userNodeService.getUserById(userId);

        if (user.getTopics() == null) {
            return List.of();
        }

        return user.getTopics().stream()
                .map(topic -> RenameTopicResponseDTO.builder()
                        .topicId(topic.getTopicId())
                        .topicName(topic.getTopicName())
                        .createdAt(topic.getCreatedAt() != null ? topic.getCreatedAt() : null)
                        .build())
                .collect(Collectors.toList());
    }

    // 토픽의 질문-답변 목록 조회
    public List<QuestionAnswerDTO> getTopicQuestionsAndAnswers(String topicId, String userId) {
        nodeUtilService.checkOwnership(topicId, userId);
        return topicRepository.findQuestionsAndAnswersByTopicId(topicId);
    }

    // 토픽의 질문-답변 목록 Map 조회
    public TopicTreeMapResponseDTO getTopicQuestionsMap(String topicId, String userId) {
        nodeUtilService.checkOwnership(topicId, userId);
        List<QuestionAnswerDTO> flatList = getTopicQuestionsAndAnswers(topicId, userId);
        boolean includeTopicNode = true;

        // 맵 변환 유틸 호출
        return nodeUtilService.buildMapFromFlatList(flatList, topicId, includeTopicNode);
    }

    // 토픽 삭제
    public void deleteTopic(String topicId, String userId) {
        nodeUtilService.checkOwnership(topicId, userId);
        topicRepository.deleteById(topicId);
    }
}
