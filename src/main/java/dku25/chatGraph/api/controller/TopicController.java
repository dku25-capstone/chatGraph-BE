package dku25.chatGraph.api.controller;

import dku25.chatGraph.api.graph.dto.QuestionAnswerDTO;
import dku25.chatGraph.api.graph.dto.QuestionTreeNodeDTO;
import dku25.chatGraph.api.graph.dto.RenameTopicRequestDTO;
import dku25.chatGraph.api.graph.dto.TopicResponseDTO;
import dku25.chatGraph.api.graph.service.GraphService;
import dku25.chatGraph.api.graph.service.TopicService;
import dku25.chatGraph.api.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Topic", description = "토픽 노드 API")
@RestController
@RequestMapping("/api/topics")
public class TopicController extends BaseController {
    private final GraphService graphService;
    private final TopicService topicService;

    public TopicController(GraphService graphService, TopicService topicService) {
        this.graphService = graphService;
        this.topicService = topicService;
    }

    @Operation(summary = "토픽 목록 조회", description = "유저 ID 활용한 자신의 토픽 목록 조회")
    @GetMapping("/history")
    public ResponseEntity<List<TopicResponseDTO>> getMyTopics(@AuthenticationPrincipal CustomUserDetails userDetails) {
        List<TopicResponseDTO> topics = topicService.getTopicsByUserId(getUserId(userDetails));
        return ResponseEntity.ok(topics);
    }

    @Operation(summary = "해당 토픽의 문답 모두 조회", description = "토픽 ID를 활용한 해당 토픽 내의 모든 문답 조회")
    @GetMapping("/{topicId}")
    public ResponseEntity<List<QuestionAnswerDTO>> getTopicQuestions(
            @PathVariable String topicId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<QuestionAnswerDTO> questions = topicService.getTopicQuestionsAndAnswers(topicId, getUserId(userDetails));
        return ResponseEntity.ok(questions);
    }

    @Operation(summary = "해당 토픽의 문답 트리 조회", description = "토픽 ID를 활용한 해당 토픽 내의 문답 트리 조회")
    @GetMapping("/{topicId}/tree")
    public ResponseEntity<List<QuestionTreeNodeDTO>> getTopicQuestionsTree(
            @PathVariable String topicId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<QuestionTreeNodeDTO> treeList = topicService.getTopicQuestionsTree(topicId, getUserId(userDetails));
        return ResponseEntity.ok(treeList);
    }


    @Operation(summary = "토픽명 수정")
    @PatchMapping("/{topicId}")
    public ResponseEntity<TopicResponseDTO> renameTopic(
            @PathVariable String topicId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody RenameTopicRequestDTO req) {
        String newTopicName = req.getNewTopicName();
        TopicResponseDTO topic = topicService.renameTopic(topicId, getUserId(userDetails), newTopicName);
        return ResponseEntity.ok(topic);
    }

    @Operation(summary = "토픽 삭제", description = "해당 토픽 삭제 전 내부 모든 데이터 삭제 됨을 알려야 함")
    @DeleteMapping("/{topicId}")
    public ResponseEntity<Void> deleteTopic(
            @PathVariable String topicId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        topicService.deleteTopic(topicId, getUserId(userDetails));
        return ResponseEntity.noContent().build();
    }
}
