package dku25.chatGraph.api.openai.service;

import com.openai.client.OpenAIClient;
import com.openai.models.ChatModel;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.chat.completions.ChatCompletionMessageParam;
import com.openai.models.chat.completions.ChatCompletionUserMessageParam;
import com.openai.models.chat.completions.ChatCompletionAssistantMessageParam;

import dku25.chatGraph.api.graph.dto.QuestionAnswerDTO;
import dku25.chatGraph.api.graph.dto.QuestionNodeMapDTO;
import dku25.chatGraph.api.graph.dto.TopicNodeDTO;
import dku25.chatGraph.api.graph.dto.TopicTreeMapResponseDTO;
import dku25.chatGraph.api.graph.node.QuestionNode;
import dku25.chatGraph.api.graph.repository.QuestionRepository;
import dku25.chatGraph.api.graph.repository.TopicRepository;
import dku25.chatGraph.api.graph.service.GraphService;

import dku25.chatGraph.api.graph.service.NodeUtilService;
import dku25.chatGraph.api.graph.service.QuestionService;
import dku25.chatGraph.api.graph.service.TopicService;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.*;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class OpenaiService {

    private final Logger logger = LoggerFactory.getLogger(OpenaiService.class);
    private final OpenAIClient openaiClient;
    private final ChatModel defaultModel;
    private final GraphService graphService;
    private final QuestionRepository questionRepository;
    private final QuestionService questionService;
    private final TopicRepository topicRepository;
    private final TopicService topicService;
    private final NodeUtilService nodeUtilService;

    @Autowired
    public OpenaiService(
            OpenAIClient openaiClient,
            @Value("${openai.model.default}") ChatModel defaultModel,
            GraphService graphService, QuestionRepository questionRepository, QuestionService questionService, TopicRepository topicRepository, TopicService topicService,
            NodeUtilService nodeUtilService) {
        this.openaiClient = openaiClient;
        this.defaultModel = defaultModel;
        this.graphService = graphService;
        this.questionRepository = questionRepository;
        this.questionService = questionService;
        this.topicRepository = topicRepository;
        this.topicService = topicService;
        this.nodeUtilService = nodeUtilService;
    }

    /**
     * 이전 질문 문맥을 붙여 OpenAI에 동기 요청을 보내고,
     * 응답을 그래프 DB에 저장한 뒤 답변 문자열을 반환합니다.
     */
    public TopicTreeMapResponseDTO askWithContext(String userId, String prompt, String previousQuestionId) {
        // 1) 요청 빌더 초기화 (모델 지정)
        ChatCompletionCreateParams.Builder builder = ChatCompletionCreateParams.builder()
                .model(defaultModel);

        // 2) 이전 문맥(chain) 메시지 추가
        Optional<QuestionNode> prevOpt = questionService.findQuestionNodeById(previousQuestionId);
        if (prevOpt.isPresent()) {
            List<QuestionNode> chain = collectContextChain(prevOpt.get());
            for (QuestionNode node : chain) {
                // assistant 역할 메시지
                if (node.getAnswer() != null) {
                    builder.addMessage(
                            ChatCompletionMessageParam.ofAssistant(
                                    ChatCompletionAssistantMessageParam.builder()
                                            .content(node.getAnswer().getText())
                                            .build()
                            )
                    );
                }
                // user 역할 메시지
                builder.addMessage(
                        ChatCompletionMessageParam.ofUser(
                                ChatCompletionUserMessageParam.builder()
                                        .content(node.getText())
                                        .build()
                        )
                );
            }
        }

        // 3) 현재 사용자 질문 추가
        builder.addMessage(
                ChatCompletionMessageParam.ofUser(
                        ChatCompletionUserMessageParam.builder()
                                .content(prompt)
                                .build()
                )
        );

        ChatCompletionCreateParams params = builder.build();

        // 4) 동기 호출
        ChatCompletion completion;
        try {
            logger.info("OpenAI 요청: model={}, messages={}", defaultModel, params.messages().size());
            completion = openaiClient
                    .chat()
                    .completions()
                    .create(params);
        } catch (Exception ex) {
            logger.error("OpenAI 요청 실패", ex);
            throw new RuntimeException("OpenAI 호출 오류", ex);
        }

        // 5) 첫 번째 응답 추출
        String answer = completion
                .choices().get(0)
                .message().content()
                .orElse("답변을 받지 못했습니다.");

        logger.info("OpenAI 응답: {}", answer);

        // 6) 그래프 DB에 동기 저장
        QuestionNode saved = graphService.saveQuestionAndAnswer(prompt, userId, answer, previousQuestionId);

        String topicId = topicRepository.findTopicIdByQuestionId(saved.getQuestionId()).orElse(null);

        // 7) 첫 질문인지, 이후 질문인지 파악 후 응답 형식 변경
        if (previousQuestionId == null) {
            return topicService.getTopicQuestionsMap(topicId, userId);
        } else {
            List<QuestionAnswerDTO> flatList = getQuestionAnswerDTO(saved);
            return nodeUtilService.buildMapFromFlatList(flatList, topicId, false);
        }
    }

    @NotNull
    private static List<QuestionAnswerDTO> getQuestionAnswerDTO(QuestionNode saved) {
        QuestionAnswerDTO addedQuestion = new QuestionAnswerDTO(
                saved.getQuestionId(),
                saved.getText(),
                saved.getLevel(),
                saved.getAnswer().getAnswerId(),
                saved.getAnswer().getText(),
                saved.getCreatedAt(),
                saved.getFollowedBy() != null ? saved.getFollowedBy().getQuestionId() : null,
                new ArrayList<>() // 자식 없음
        );

        List<QuestionAnswerDTO> flatList = List.of(addedQuestion);
        return flatList;
    }

    /**
     * QuestionNode 체인을 루트→현재 순으로 수집 (최대 3단계)
     */
    private List<QuestionNode> collectContextChain(QuestionNode node) {
        Deque<QuestionNode> stack = new ArrayDeque<>();
        int minLevel = Math.max(1, node.getLevel() - 2);
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
