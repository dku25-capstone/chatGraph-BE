package dku25.chatGraph.api.controller;

import dku25.chatGraph.api.graph.dto.QuestionResponseDTO;
import dku25.chatGraph.api.graph.dto.RenameQuestionRequestDTO;
import dku25.chatGraph.api.graph.service.GraphService;
import dku25.chatGraph.api.graph.service.QuestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @Operation(summary = "질문명 수정")
    @PatchMapping("/{questionId}")
    public ResponseEntity<QuestionResponseDTO> renameQuestion(
            @PathVariable String questionId,
            @RequestBody RenameQuestionRequestDTO dto
    ) {
        QuestionResponseDTO question = questionService.renameQuestion(questionId, dto.getNewQuestionName());
        return ResponseEntity.ok(question);
    }

    @Operation(summary = "해당 질문노드 삭제")
    @DeleteMapping("/{questionId}")
    public ResponseEntity<Void> deleteQuestion(@PathVariable String questionId) {
        questionService.deleteQuestionNode(questionId);
        return ResponseEntity.noContent().build();
    }
}
