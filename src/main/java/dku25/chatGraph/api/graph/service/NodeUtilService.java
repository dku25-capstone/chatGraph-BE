package dku25.chatGraph.api.graph.service;

import dku25.chatGraph.api.exception.InvalidNodeException;
import dku25.chatGraph.api.exception.ResourceNotFoundException;
import dku25.chatGraph.api.exception.UnauthorizedAccessException;
import dku25.chatGraph.api.graph.dto.QuestionAnswerDTO;
import dku25.chatGraph.api.graph.dto.TopicNodeDTO;
import dku25.chatGraph.api.graph.dto.TopicTreeMapResponseDTO;
import dku25.chatGraph.api.graph.node.TopicNode;
import dku25.chatGraph.api.graph.repository.AnswerRepository;
import dku25.chatGraph.api.graph.repository.QuestionRepository;
import dku25.chatGraph.api.graph.repository.TopicRepository;
import dku25.chatGraph.api.graph.repository.UserNodeRepository;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class NodeUtilService {
    private final QuestionRepository questionRepository;
    private final TopicRepository topicRepository;

    public NodeUtilService(AnswerRepository answerRepository, QuestionRepository questionRepository, TopicRepository topicRepository, UserNodeRepository userNodeRepository) {
        this.questionRepository = questionRepository;
        this.topicRepository = topicRepository;
    }

    // 토픽내 질문 목록 계층 구조로 변환
    public TopicTreeMapResponseDTO buildMapFromFlatList(List<QuestionAnswerDTO> flatList, String topicId, boolean includeTopicNode) {
        Map<String, Object> nodeMap = new LinkedHashMap<>();
        // includeTopicNode == True -> 토픽 노드 추가
        if (includeTopicNode) {
            TopicNodeDTO topicNode = topicRepository.findTopicNodeDTOById(topicId).orElseThrow(() -> new ResourceNotFoundException("토픽을 찾을 수 없습니다."));
            nodeMap.put(topicNode.getTopicId(), topicNode);
        }
        // 질문 노드 추가
        for (QuestionAnswerDTO dto : flatList) {
            nodeMap.put(dto.getQuestionId(), dto);
        }
        return new TopicTreeMapResponseDTO(topicId, nodeMap);
    }

    // 사용자가 해당 토픽, 질문의 소유자인지 확인
    public void checkOwnership(String nodeId, String userId) {
        if (nodeId.startsWith("topic-")){
            TopicNode topic = topicRepository.findById(nodeId)
                    .orElseThrow(() -> new ResourceNotFoundException("토픽을 찾을 수 없습니다."));
            if (topic.getUser() == null || !topic.getUser().getUserId().equals(userId)) {
                throw new UnauthorizedAccessException("해당 토픽에 대한 접근 권한이 없습니다.");
            }
        } else if (nodeId.startsWith("question-")) {
            if (questionRepository.findById(nodeId).isEmpty()) {
                throw new ResourceNotFoundException("질문을 찾을 수 없습니다.");
            }

            Optional<String> ownerId = questionRepository.findUserIdByQuestionId(nodeId);
            if (ownerId.isEmpty() || !ownerId.get().equals(userId)) {
                throw new UnauthorizedAccessException("해당 질문에 대한 접근 권한이 없습니다.");
            }
        } else {
            throw new InvalidNodeException("지원하지 않는 node 입니다. " + nodeId);
        }
    }
}
