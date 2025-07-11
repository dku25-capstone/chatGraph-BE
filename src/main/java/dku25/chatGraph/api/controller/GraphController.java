package dku25.chatGraph.api.controller;

import dku25.chatGraph.api.graph.dto.TopicResponseDTO;
import dku25.chatGraph.api.graph.dto.QuestionAnswerDTO;
import dku25.chatGraph.api.graph.service.GraphService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

@RestController
public class GraphController {

    private final GraphService graphService;

    public GraphController(GraphService graphService) {
        this.graphService = graphService;
    }

    @GetMapping("/topics/history")
    public ResponseEntity<List<TopicResponseDTO>> getMyTopics(Principal principal) {
        String userId = principal.getName(); // 인증된 사용자 ID 추출
        List<TopicResponseDTO> topics = graphService.getTopicsByUserId(userId);
        return ResponseEntity.ok(topics);
    }

    @GetMapping("/topics/{topicId}")
    public ResponseEntity<List<QuestionAnswerDTO>> getTopicQuestions(
            @PathVariable String topicId, 
            Principal principal) {
        String userId = principal.getName();
        List<QuestionAnswerDTO> questions = graphService.getTopicQuestionsAndAnswers(topicId, userId);
        return ResponseEntity.ok(questions);
    }
} 