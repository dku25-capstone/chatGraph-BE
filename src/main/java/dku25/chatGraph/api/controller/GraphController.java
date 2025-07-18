package dku25.chatGraph.api.controller;

import dku25.chatGraph.api.graph.dto.RenameTopicRequestDTO;
import dku25.chatGraph.api.graph.dto.TopicResponseDTO;
import dku25.chatGraph.api.graph.dto.QuestionAnswerDTO;
import dku25.chatGraph.api.graph.service.GraphService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @PatchMapping("/topics/{topicId}")
    public ResponseEntity<TopicResponseDTO> renameTopic(
            @PathVariable String topicId,
            Principal principal,
            @RequestBody RenameTopicRequestDTO req) {
        String userId = principal.getName();
        String newTopicName = req.getNewTopicName();
        TopicResponseDTO topic = graphService.renameTopic(topicId, userId, newTopicName);
        return ResponseEntity.ok(topic);
    }

    @DeleteMapping("/topics/{topicId}")
    public ResponseEntity<Void> deleteTopic(
            @PathVariable String topicId,
            Principal principal) {
        String userId = principal.getName();
        graphService.deleteTopic(topicId, userId);
        return ResponseEntity.noContent().build();
    }
} 