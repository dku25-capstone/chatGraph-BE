package dku25.chatGraph.api.controller;

import dku25.chatGraph.api.openai.dto.AskRequest;
import dku25.chatGraph.api.openai.dto.AskResponse;
import dku25.chatGraph.api.openai.service.OpenaiService;
import dku25.chatGraph.api.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "OpenAI", description = "OpenAI API")
@RestController
@RequestMapping("/api/ask-context")
public class OpenaiController extends BaseController{

    private final Logger logger = LoggerFactory.getLogger(OpenaiController.class);
    private final OpenaiService openaiService;

    public OpenaiController(OpenaiService openaiService) {
        this.openaiService = openaiService;
    }

    @Operation(summary = "OpenAI API", description = "외부 OpenAI API에 요청 및 응답")
    @PostMapping
    public ResponseEntity<AskResponse> askWithContext(
            @Valid @RequestBody AskRequest req,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        logger.info("/ask-context 요청 - userId: {}, 질문: {}, 이전 질문ID: {}",
                getUserId(userDetails), req.getPrompt(), req.getPreviousQuestionId());

        // 서비스 호출 (동기)
        AskResponse resp = openaiService.askWithContext(getUserId(userDetails), req.getPrompt(), req.getPreviousQuestionId());

        return ResponseEntity.ok(resp);
    }
}
