package dku25.chatGraph.api.controller;

import dku25.chatGraph.api.openai.dto.AskRequest;
import dku25.chatGraph.api.openai.dto.AskResponse;
import dku25.chatGraph.api.openai.service.OpenaiService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/ask-context")
public class OpenaiController {

    private final Logger logger = LoggerFactory.getLogger(OpenaiController.class);
    private final OpenaiService openaiService;

    public OpenaiController(OpenaiService openaiService) {
        this.openaiService = openaiService;
    }

    @PostMapping
    public ResponseEntity<AskResponse> askWithContext(
            @Valid @RequestBody AskRequest req,
            Principal principal
    ) {
        String userId = principal.getName();
        logger.info("/ask-context 요청 - userId: {}, 질문: {}, 이전 질문ID: {}",
                userId, req.getPrompt(), req.getPreviousQuestionId());

        // 서비스 호출 (동기)
        AskResponse resp = openaiService.askWithContext(userId, req.getPrompt(), req.getPreviousQuestionId());

        return ResponseEntity.ok(resp);
    }
}
