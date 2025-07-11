package dku25.chatGraph.api.controller;

import dku25.chatGraph.api.graph.dto.TopicResponseDTO;
import dku25.chatGraph.api.user.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

@RestController
public class GraphController {

    private final UserService userService;

    public GraphController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/topics/history")
    public ResponseEntity<List<TopicResponseDTO>> getMyTopics(Principal principal) {
        String userId = principal.getName(); // 인증된 사용자 ID 추출
        List<TopicResponseDTO> topics = userService.getTopicsByUserId(userId);
        return ResponseEntity.ok(topics);
    }
} 