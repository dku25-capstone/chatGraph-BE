package dku25.chatGraph.api.controller;

import dku25.chatGraph.api.graph.dto.NodeRenameRequestDTO;
import dku25.chatGraph.api.graph.dto.NodeRenameResponseDTO;
import dku25.chatGraph.api.graph.dto.PartialCopyRequestDTO;
import dku25.chatGraph.api.graph.dto.PartialCopyResponseDTO;
import dku25.chatGraph.api.graph.dto.QuestionAnswerDTO;
import dku25.chatGraph.api.graph.service.GraphService;
import dku25.chatGraph.api.graph.service.QuestionService;
import dku25.chatGraph.api.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Question", description = "질문 노드 API")
@RestController
@RequestMapping("/api/questions")
public class QuestionController extends BaseController {
    private final GraphService graphService;
    private final QuestionService questionService;

    public QuestionController(GraphService graphService, QuestionService questionService) {
        this.graphService = graphService;
        this.questionService = questionService;
    }

    @GetMapping("/search")
    public ResponseEntity<List<QuestionAnswerDTO>> searchQuestions(
            @RequestParam String keyword,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<QuestionAnswerDTO> questionsAndAnswers = questionService.searchByKeyword(keyword, getUserId(userDetails));
        return ResponseEntity.ok(questionsAndAnswers);
    }

    @Operation(summary = "질문명 수정", description = "토픽명 변경과 로직과 반환이 같음 nodeType 확인")
    @PatchMapping("/{questionId}")
    public ResponseEntity<NodeRenameResponseDTO> renameQuestion(
            @PathVariable String questionId,
            @RequestBody NodeRenameRequestDTO dto,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(graphService.renameNode(questionId, getUserId(userDetails), dto.getNewNodeName()));
    }

    @Operation(summary = "해당 질문노드 삭제")
    @DeleteMapping("/{questionId}")
    public ResponseEntity<Void> deleteQuestion(
            @PathVariable String questionId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        List<String> singleQuestionId = List.of(questionId);
        questionService.deleteQuestionNode(singleQuestionId, getUserId(userDetails));
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "복수 질문노드 삭제")
    @DeleteMapping("/batch")
    public ResponseEntity<Void> deleteQuestions(
            @RequestBody List<String> questionIds,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        questionService.deleteQuestionNode(questionIds, getUserId(userDetails));
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "선택 질문 노드(단일, 복수) 복제",
            description = "그래프 화면 상에서 선택한 질문 노드들의 트리를 특정 질문 노드의 하위 노드로 이동\n" +
                    "* 복수 질문 선택 시 복제하고자 하는 최상위 질문부터 모든 하위 질문은 연결되어 있어야 함.")
    @PostMapping("/partial-copy")
    public ResponseEntity<PartialCopyResponseDTO> copyPartialTree(
            @RequestBody PartialCopyRequestDTO req,
            @AuthenticationPrincipal CustomUserDetails userDetails
            ) {
        List<String> newIds = questionService.copyQuestionNodes(
                req.getSourceQuestionIds(),
                req.getTargetParentId(),
                userDetails.getUserId()
        );
        return ResponseEntity.ok(new PartialCopyResponseDTO(newIds));
    }
}
