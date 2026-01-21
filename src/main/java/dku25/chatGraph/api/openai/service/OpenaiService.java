package dku25.chatGraph.api.openai.service;

import com.openai.client.OpenAIClient;
import com.openai.models.ChatModel;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionAssistantMessageParam;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.chat.completions.ChatCompletionMessageParam;
import com.openai.models.chat.completions.ChatCompletionUserMessageParam;
import dku25.chatGraph.api.exception.ExternalApiException;
import dku25.chatGraph.api.graph.dto.QuestionAnswerDTO;
import dku25.chatGraph.api.graph.dto.TopicTreeMapResponseDTO;
import dku25.chatGraph.api.graph.node.QuestionNode;
import dku25.chatGraph.api.graph.repository.QuestionRepository;
import dku25.chatGraph.api.graph.repository.TopicRepository;
import dku25.chatGraph.api.graph.service.GraphService;
import dku25.chatGraph.api.graph.service.NodeUtilService;
import dku25.chatGraph.api.graph.service.QuestionService;
import dku25.chatGraph.api.graph.service.TopicService;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class OpenaiService {

    private final Logger logger = LoggerFactory.getLogger(OpenaiService.class);
    private final OpenAIClient openaiClient;
    private final ChatModel defaultModel;
    private final GraphService graphService;
    private final QuestionRepository questionRepository;
    private final TopicRepository topicRepository;
    private final TopicService topicService;
    private final NodeUtilService nodeUtilService;

    public OpenaiService(
            OpenAIClient openaiClient, @Value("${openai.model.default}") ChatModel defaultModel,
            GraphService graphService, QuestionRepository questionRepository, QuestionService questionService,
            TopicRepository topicRepository, TopicService topicService, NodeUtilService nodeUtilService) {
        this.openaiClient = openaiClient;
        this.defaultModel = defaultModel;
        this.graphService = graphService;
        this.questionRepository = questionRepository;
        this.topicRepository = topicRepository;
        this.topicService = topicService;
        this.nodeUtilService = nodeUtilService;
    }

    public TopicTreeMapResponseDTO askWithContext(String userId, String prompt, String previousQuestionId) {
        // 1. OpenAI에 보낼 메시지 리스트(문맥 포함) 구성
        List<ChatCompletionMessageParam> messages = buildMessageParams(prompt, previousQuestionId);

        // 2. OpenAI 답변 획득
        String answer = executeChatCompletion(messages, "Main Answer");

        // 3. 신규 토픽인 경우 요약 생성 (기존 분기 로직 보존)
        String topicSummary = null;
        if (previousQuestionId == null) {
            topicSummary = generateTopicSummary(prompt);
        }

        // 4. 그래프 DB 저장
        QuestionNode savedNode = graphService.saveQuestionAndAnswer(prompt, userId, answer, previousQuestionId,
                topicSummary);

        // 5. 최종 응답 DTO 생성 및 반환
        return buildFinalResponse(savedNode, userId, previousQuestionId == null);
    }

    private List<ChatCompletionMessageParam> buildMessageParams(String prompt, String previousQuestionId) {
        List<ChatCompletionMessageParam> messages = new ArrayList<>();

        // 이전 문맥 추가
        questionRepository.findById(previousQuestionId).ifPresent(prevNode -> {
            List<QuestionNode> chain = collectContextChain(prevNode);
            for (QuestionNode node : chain) {
                messages.add(ChatCompletionMessageParam.ofUser(
                        ChatCompletionUserMessageParam.builder().content(node.getText()).build()));

                if (node.getAnswer() != null) {
                    messages.add(ChatCompletionMessageParam.ofAssistant(
                            ChatCompletionAssistantMessageParam.builder().content(node.getAnswer().getText()).build()));
                }
            }
        });

        // 현재 질문 추가
        messages.add(ChatCompletionMessageParam.ofUser(
                ChatCompletionUserMessageParam.builder().content(prompt).build()));

        return messages;
    }

    private String executeChatCompletion(List<ChatCompletionMessageParam> messages, String taskName) {
        ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                .model(defaultModel)
                .messages(messages)
                .build();

        try {
            logger.info("OpenAI API Request [{}]: model={}, messagesCount={}", taskName, defaultModel, messages.size());
            ChatCompletion completion = openaiClient.chat().completions().create(params);

            return completion.choices().getFirst().message().content()
                    .orElseThrow(() -> new ExternalApiException("OpenAI 응답 내용을 추출할 수 없습니다."));
        } catch (Exception ex) {
            logger.error("OpenAI API Error [{}]: ", taskName, ex);
            throw new ExternalApiException("OpenAI 호출 중 오류가 발생했습니다: " + taskName, ex);
        }
    }

    private String generateTopicSummary(String prompt) {
        String summaryPrompt = prompt + "\n(이 질문을 토픽 형태로 20자 이내로 요약해줘. 마침표 없이)";
        List<ChatCompletionMessageParam> messages = List.of(
                ChatCompletionMessageParam.ofUser(
                        ChatCompletionUserMessageParam.builder().content(summaryPrompt).build())
        );
        return executeChatCompletion(messages, "Topic Summary");
    }

    private TopicTreeMapResponseDTO buildFinalResponse(QuestionNode savedNode, String userId, boolean isNewTopic) {
        String topicId = topicRepository.findTopicIdByQuestionId(savedNode.getQuestionId()).orElse(null);

        if (isNewTopic) {
            // 새 토픽인 경우 전체 맵 조회
            return topicService.getTopicQuestionsMap(topicId, userId);
        }
        // 기존 토픽의 하위 질문인 경우 단일 노드 맵 구성
        List<QuestionAnswerDTO> flatList = List.of(convertToDTO(savedNode));
        return nodeUtilService.buildMapFromFlatList(flatList, topicId, false);
    }

    private QuestionAnswerDTO convertToDTO(QuestionNode node) {
        return new QuestionAnswerDTO(
                node.getQuestionId(),
                node.getText(),
                node.getLevel(),
                node.isFavorite(),
                node.getAnswer().getAnswerId(),
                node.getAnswer().getText(),
                node.getCreatedAt(),
                new ArrayList<>()
        );
    }

    /**
     * QuestionNode 체인을 루트→현재 순으로 수집 (최대 5단계)
     */
    private List<QuestionNode> collectContextChain(QuestionNode node) {
        Deque<QuestionNode> stack = new ArrayDeque<>();
        int minLevel = Math.max(1, node.getLevel() - 4);
        QuestionNode cursor = node;

        while (cursor != null && cursor.getLevel() >= minLevel) {
            stack.push(cursor);
            cursor = questionRepository.getPreviousQuestion(cursor.getQuestionId());
        }

        List<QuestionNode> chain = new ArrayList<>();
        while (!stack.isEmpty()) {
            chain.add(stack.pop());
        }
        return chain;
    }
}
