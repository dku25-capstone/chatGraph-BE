package dku25.chatGraph.api.service;

import dku25.chatGraph.api.graph.AnswerNode;
import dku25.chatGraph.api.graph.AnswerRepository;
import dku25.chatGraph.api.graph.QuestionNode;
import dku25.chatGraph.api.graph.QuestionRepository;
import dku25.chatGraph.api.model.ChatCompletionRequest;
import dku25.chatGraph.api.model.ChatCompletionResponse;
import dku25.chatGraph.api.model.Message;
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
    private final Map<String, List<Message>> conversationHistory = new HashMap<>(); // 세션별 대화 기록 저장소

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private AnswerRepository answerRepository;

    @Autowired
    public OpenaiService(@Qualifier("openaiWebClient") WebClient openaiWebClient,
                         @Value("${openai.model.default}") String defaultOpenaiModel) {
        this.openaiWebClient = openaiWebClient;
        this.defaultOpenaiModel = defaultOpenaiModel;
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

    public Mono<ChatCompletionResponse> getChatCompletion(List<Message> messages) {
        return getChatCompletion(messages, this.defaultOpenaiModel);
    }

    public String startNewChat() {
        String sessionId = UUID.randomUUID().toString();
        conversationHistory.put(sessionId, new ArrayList<>());
        logger.info("새로운 채팅 세션 시작: {}", sessionId);
        return sessionId;
    }

    public Mono<String> askWithContext(String sessionId, String prompt) {
        List<Message> messages = conversationHistory.computeIfAbsent(sessionId, k -> new ArrayList<>());
        messages.add(new Message("user", prompt));
        logger.info("세션 {} 에 새 질문 추가: {}", sessionId, prompt);

        return getChatCompletion(messages)
                .map(response -> {
                    if (response != null && response.getChoices() != null && !response.getChoices().isEmpty()) {
                        String answer = response.getChoices().get(0).getMessage().getContent();
                        messages.add(new Message("assistant", answer)); // 답변을 대화 기록에 추가
                        logger.info("세션 {} 에 답변 추가: {}", sessionId, answer);
                        System.out.println("Neo4J 연결 테스트: " + questionRepository.count());
                        saveToNeo4j(sessionId, prompt, answer);
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

    public void saveToNeo4j(String sessionId, String prompt, String answer) {
        AnswerNode answerNode = new AnswerNode();
        answerNode.setText(answer);
        answerRepository.save(answerNode);

        QuestionNode questionNode = new QuestionNode();
        questionNode.setText(prompt);
        questionNode.setSessionId(sessionId);
        questionNode.setAnswer(answerNode);
        questionRepository.save(questionNode);
    }
}