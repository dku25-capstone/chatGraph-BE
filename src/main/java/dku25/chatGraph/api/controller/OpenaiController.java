package dku25.chatGraph.api.controller;

import dku25.chatGraph.api.openai.service.OpenaiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;
import org.springframework.security.core.Authentication;

@RestController
public class OpenaiController {

    private final Logger logger = LoggerFactory.getLogger(OpenaiController.class);
    private final OpenaiService openaiService;

    @Autowired
    public OpenaiController(OpenaiService openaiService) {
        this.openaiService = openaiService;
    }

    @PostMapping("/ask-context")
    public Mono<String> askWithContext(
        @RequestBody Map<String, String> payload,
        Authentication authentication
    ) {
        String userId = (String) authentication.getPrincipal();
        String prompt = payload.get("prompt");
        String previousQuestionId = payload.get("previousQuestionId");
        logger.info("/ask-context 요청 - userId: {}, 질문: {}, 이전 질문ID: {}", userId, prompt, previousQuestionId);

        if (prompt == null || prompt.trim().isEmpty()) {
            logger.warn("/ask-context 요청 - 질문이 비어 있습니다.");
            return Mono.just("질문을 입력해주세요.");
        }

        // userId를 서비스에 넘겨서 처리
        return openaiService.askWithContext(userId, prompt, previousQuestionId);
    }
}
