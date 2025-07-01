package dku25.chatGraph.api.controller;

import dku25.chatGraph.api.openai.service.OpenaiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
public class OpenaiController {
    private final Logger logger = LoggerFactory.getLogger(OpenaiController.class);
    private final OpenaiService openaiService;

    @Autowired
    public OpenaiController(OpenaiService openaiService) {
        this.openaiService = openaiService;
    }

    @PostMapping("/new-chat")
    public Mono<String> newChat() {
        String sessionId = openaiService.startNewChat();
        logger.info("/new-chat 요청 - 세션 ID: {}", sessionId);
        return Mono.just(sessionId);
    }

    @PostMapping("/ask-context")
    public Mono<String> askWithContext(@RequestParam String sessionId, @RequestBody Map<String, String> payload) {
        String prompt = payload.get("prompt");
        String previousQuestionId = payload.get("previousQuestionId");
        logger.info("/ask-context 요청 - 세션 ID: {}, 질문: {}, 이전 질문ID: {}", sessionId, prompt, previousQuestionId);

        if (prompt == null || prompt.isEmpty()) {
            logger.warn("/ask-context 요청 - 질문이 비어 있습니다.");
            return Mono.just("질문을 입력해주세요.");
        }
        if (previousQuestionId != null && !previousQuestionId.isEmpty()) {
            return openaiService.askWithContext(sessionId, prompt,  previousQuestionId)
                    .doOnSuccess(answer -> logger.info("/ask-context 응답 - 세션 ID: {}, 답변: {}", sessionId, answer));
        }

        return openaiService.askWithContext(sessionId, prompt)
                .doOnSuccess(answer -> logger.info("/ask-context 응답 - 세션 ID: {}, 답변: {}", sessionId, answer));
    }
}