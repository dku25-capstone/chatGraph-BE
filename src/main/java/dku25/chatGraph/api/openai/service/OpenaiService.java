package dku25.chatGraph.api.openai.service;

import dku25.chatGraph.api.graph.node.AnswerNode;
import dku25.chatGraph.api.graph.repository.AnswerRepository;
import dku25.chatGraph.api.graph.node.QuestionNode;
import dku25.chatGraph.api.graph.repository.QuestionRepository;
import dku25.chatGraph.api.graph.service.GraphService;
import dku25.chatGraph.api.openai.model.ChatCompletionRequest;
import dku25.chatGraph.api.openai.model.ChatCompletionResponse;
import dku25.chatGraph.api.openai.model.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
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
    private final Map<String, List<Message>> conversationHistory = new HashMap<>(); // 세션별 대화 기록 저장소

    @Autowired
    public OpenaiService(@Qualifier("openaiWebClient") WebClient openaiWebClient,
                         @Value("${openai.model.default}") String defaultOpenaiModel, GraphService graphService) {
        this.openaiWebClient = openaiWebClient;
        this.defaultOpenaiModel = defaultOpenaiModel;
        this.graphService = graphService;
    }

    public String startNewChat() {
        String sessionId = UUID.randomUUID().toString();
        conversationHistory.put(sessionId, new ArrayList<>());
        logger.info("새로운 채팅 세션 시작: {}", sessionId);
        return sessionId;
    }

    public Mono<String> askWithContext(String sessionId, String prompt) {
        return askWithOptionalPrevious(sessionId, prompt, Optional.empty());
    }

    public Mono<String> askWithContext(String sessionId, String prompt, String previousQuestionId) {
        Optional<QuestionNode> previous = graphService.findQuestionById(previousQuestionId);
        return askWithOptionalPrevious(sessionId, prompt, previous);
    }

    public Mono<String> askWithOptionalPrevious(String sessionId, String prompt, Optional<QuestionNode> previousOpt) {
        List<Message> messages = conversationHistory.computeIfAbsent(sessionId, k -> new ArrayList<>());
        messages.add(new Message("user", prompt));
        logger.info("세션 {} 에 새 질문 추가: {}", sessionId, prompt);

        return getChatCompletion(messages)
                .map(response -> {
                    if (response != null && response.getChoices() != null && !response.getChoices().isEmpty()) {
                        String answer = response.getChoices().get(0).getMessage().getContent();
                        messages.add(new Message("assistant", answer)); // 답변을 대화 기록에 추가
                        logger.info("세션 {} 에 답변 추가: {}", sessionId, answer);
                        graphService.saveToNeo4j(sessionId, prompt, answer, previousOpt);
                        return answer;
                    } else {
                        String errorMessage = "OpenAI API 응답이 비었습니다.";
                        logger.warn(errorMessage);
                        return errorMessage;
                    }
                })
                .onErrorResume(e -> {
                    logger.error("세션 {} 맥락 유지 요청 중 오류 발생: {}", sessionId, e.getMessage());
                    return Mono.just("맥락 유지 요청 중 오류 발생: " + e.getMessage());
                });
    }

    public Mono<ChatCompletionResponse> getChatCompletion(List<Message> messages) {
        return getChatCompletion(messages, this.defaultOpenaiModel);
    }

    public Mono<ChatCompletionResponse> getChatCompletion(List<Message> messages, String model) {
        ChatCompletionRequest request = new ChatCompletionRequest(model, messages);
        logger.info("OpenAI API 요청: {}", request);

        return openaiWebClient.post()
                .uri("/chat/completions")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(ChatCompletionResponse.class)
                .doOnSuccess(response -> logger.info("OpenAI API 응답: {}", response))
                .doOnError(error -> logger.error("OpenAI API 요청 실패: {}", error.getMessage()));
    }

}