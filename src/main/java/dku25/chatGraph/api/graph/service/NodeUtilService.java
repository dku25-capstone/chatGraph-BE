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


    public TopicTreeMapResponseDTO buildMapFromFlatList(List<QuestionAnswerDTO> flatList, TopicNodeDTO topicNode) {
        Map<String, Object> nodeMap = new LinkedHashMap<>();
        // 토픽 노드 추가
        nodeMap.put(topicNode.getTopicId(), topicNode);

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
        return new TopicTreeMapResponseDTO(topicNode.getTopicId(), nodeMap);
    }
}
