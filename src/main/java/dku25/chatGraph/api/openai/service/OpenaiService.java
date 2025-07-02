package dku25.chatGraph.api.openai.service;

import dku25.chatGraph.api.graph.node.QuestionNode;
import dku25.chatGraph.api.graph.service.GraphService;
import dku25.chatGraph.api.openai.model.ChatCompletionRequest;
import dku25.chatGraph.api.openai.model.ChatCompletionResponse;
import dku25.chatGraph.api.openai.model.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.*;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.*;

@Service
public class OpenaiService {

    private final Logger logger = LoggerFactory.getLogger(OpenaiService.class);
    private final WebClient openaiWebClient;
    private final String defaultOpenaiModel;
    private final GraphService graphService;

    @Autowired
    public OpenaiService(@Qualifier("openaiWebClient") WebClient openaiWebClient,
                         @Value("${openai.model.default}") String defaultOpenaiModel,
                         GraphService graphService) {
        this.openaiWebClient = openaiWebClient;
        this.defaultOpenaiModel = defaultOpenaiModel;
        this.graphService = graphService;
    }

    public String startNewChat() {
        String sessionId = UUID.randomUUID().toString();
        logger.info("새로운 채팅 세션 시작: {}", sessionId);
        return sessionId;
    }

    public Mono<String> askWithContext(String sessionId, String prompt, String previousQuestionId) {
        List<Message> contextMessages = new ArrayList<>();

        if (previousQuestionId != null && !previousQuestionId.isEmpty()) {
            Optional<QuestionNode> prevQuestionNodeOpt = graphService.findQuestionById(previousQuestionId);
            prevQuestionNodeOpt.ifPresent(prevQuestionNode -> contextMessages.addAll(buildContextMessages(prevQuestionNode)));
        }

        contextMessages.add(new Message("user", prompt));

        return getChatCompletion(contextMessages)
                .map(response -> {
                    String answer = response.getFirstAnswerContent();
                    logger.info("응답 저장 - 세션 ID: {}, 답변: {}", sessionId, answer);

                    if (previousQuestionId != null && !previousQuestionId.isEmpty()) {
                        graphService.saveFollowUpQuestion(prompt, sessionId, answer, previousQuestionId);
                    } else {
                        graphService.saveFirstQuestion(prompt, sessionId, answer);
                    }

                    return answer;
                })
                .onErrorResume(e -> {
                    logger.error("OpenAI 요청 실패: {}", e.getMessage());
                    return Mono.just("오류가 발생했습니다: " + e.getMessage());
                });
    }

    public Mono<ChatCompletionResponse> getChatCompletion(List<Message> messages) {
        return getChatCompletion(messages, defaultOpenaiModel);
    }

    public Mono<ChatCompletionResponse> getChatCompletion(List<Message> messages, String model) {
        ChatCompletionRequest request = new ChatCompletionRequest(model, messages);
        logger.info("OpenAI API 요청: {}", request);

        return openaiWebClient.post()
                .uri("/chat/completions")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(ChatCompletionResponse.class)
                .doOnSuccess(response -> logger.info("OpenAI 응답 성공"))
                .doOnError(error -> logger.error("OpenAI 응답 실패: {}", error.getMessage()));
    }

    public List<Message> buildContextMessages(QuestionNode prevQuestionNode) {
        List<Message> messages = new ArrayList<>();
        QuestionNode cursor = prevQuestionNode;
        int cursorLevel = prevQuestionNode.getLevel();

        int targetLevel = Math.max(1, cursorLevel - 2);

        while (cursor != null && cursor.getLevel() >= targetLevel) {
            if (cursor.getAnswer() != null) {
                messages.add(0, new Message("assistant", cursor.getAnswer().getText()));
            }
            messages.add(0, new Message("user", cursor.getText()));
            cursor = cursor.getPreviousQuestion();
        }

        return messages;
    }
}
