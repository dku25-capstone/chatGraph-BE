package dku25.chatGraph.api.graph.service;

import dku25.chatGraph.api.graph.dto.QuestionAnswerDTO;
import dku25.chatGraph.api.graph.dto.QuestionNodeMapDTO;
import dku25.chatGraph.api.graph.dto.TopicNodeDTO;
import dku25.chatGraph.api.graph.dto.TopicTreeMapResponseDTO;
import dku25.chatGraph.api.graph.repository.AnswerRepository;
import dku25.chatGraph.api.graph.repository.QuestionRepository;
import dku25.chatGraph.api.graph.repository.TopicRepository;
import dku25.chatGraph.api.graph.repository.UserNodeRepository;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class NodeUtilService {
    private final AnswerRepository answerRepository;
    private final QuestionRepository questionRepository;
    private final TopicRepository topicRepository;
    private final UserNodeRepository userNodeRepository;

    public NodeUtilService(AnswerRepository answerRepository, QuestionRepository questionRepository, TopicRepository topicRepository, UserNodeRepository userNodeRepository) {
        this.answerRepository = answerRepository;
        this.questionRepository = questionRepository;
        this.topicRepository = topicRepository;
        this.userNodeRepository = userNodeRepository;
    }

    // 토픽내 질문 목록 계층 구조로 변환
    public TopicTreeMapResponseDTO buildMapFromFlatList(List<QuestionAnswerDTO> flatList, String topicId, boolean includeTopicNode) {
        Map<String, Object> nodeMap = new LinkedHashMap<>();
        // includeTopicNode == True -> 토픽 노드 추가
        if (includeTopicNode) {
            TopicNodeDTO topicNode = topicRepository.findTopicNodeDTOById(topicId).orElseThrow(() -> new RuntimeException("토픽 정보 없음"));
            nodeMap.put(topicNode.getTopicId(), topicNode);
        }
        // 질문 노드 추가
        for (QuestionAnswerDTO dto : flatList) {
            nodeMap.put(dto.getQuestionId(), new QuestionNodeMapDTO(
                    dto.getQuestionId(),
                    dto.getQuestion(),
                    dto.getLevel(),
                    dto.getAnswerId(),
                    dto.getAnswer(),
                    dto.getCreatedAt(),
                    dto.getChildren()
            ));
        }
        return new TopicTreeMapResponseDTO(topicId, nodeMap);
    }
}
