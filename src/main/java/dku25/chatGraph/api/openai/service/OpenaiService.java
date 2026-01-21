package dku25.chatGraph.api.openai.service;

import com.openai.client.OpenAIClient;
import com.openai.models.ChatModel;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.chat.completions.ChatCompletionMessageParam;
import com.openai.models.chat.completions.ChatCompletionUserMessageParam;
import com.openai.models.chat.completions.ChatCompletionAssistantMessageParam;

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
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.*;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class OpenaiService {

    private final ChatClientService chatClient;     // 통신 담당
    private final PromptManager promptManager;      // 메시지 조립 담당
    private final QuestionRepository questionRepository; // DB 조회 담당

    // 기존에 있던 서비스들
    private final GraphService graphService;
    private final TopicRepository topicRepository;
    private final TopicService topicService;
    private final NodeUtilService nodeUtilService;

    public OpenaiService(ChatClientService chatClient, PromptManager promptManager, QuestionRepository questionRepository, GraphService graphService, TopicRepository topicRepository, TopicService topicService, NodeUtilService nodeUtilService) {
        this.chatClient = chatClient;
        this.promptManager = promptManager;
        this.questionRepository = questionRepository;
        this.graphService = graphService;
        this.topicRepository = topicRepository;
        this.topicService = topicService;
        this.nodeUtilService = nodeUtilService;
    }

    public TopicTreeMapResponseDTO askWithContext(String userId, String prompt, String prevId) {
        // 1. 문맥 조회 (prevId가 null이면 빈 리스트 반환 - if문 제거)
        List<QuestionNode> chain = (prevId == null)
                ? Collections.emptyList()
                : questionRepository.findContextChain(prevId);

        // 2. AI에게 질문 (PromptManager가 메시지 만들고, ChatClient가 통신)
        String aiAnswer = chatClient.ask(promptManager.createMessages(chain, prompt));

        // 3. 결과 저장 및 반환 (첫 질문 여부에 따라 분기)
        return (prevId == null)
                ? processFirstQuestion(userId, prompt, aiAnswer)
                : processFollowUpQuestion(userId, prompt, aiAnswer, prevId);
    }

    // --- 하위 메서드 분리 ---

    private TopicTreeMapResponseDTO processFirstQuestion(String userId, String prompt, String answer) {
        // 요약 생성도 ChatClient에게 위임
        String summary = chatClient.ask(promptManager.createSummaryMessages(prompt));

        QuestionNode saved = graphService.saveQuestionAndAnswer(prompt, userId, answer, null, summary);

        // TopicService를 통해 맵 반환
        return topicRepository.findTopicIdByQuestionId(saved.getQuestionId())
                .map(topicId -> topicService.getTopicQuestionsMap(topicId, userId))
                .orElseThrow(() -> new RuntimeException("토픽 생성 실패"));
    }

    private TopicTreeMapResponseDTO processFollowUpQuestion(String userId, String prompt, String answer, String prevId) {
        QuestionNode saved = graphService.saveQuestionAndAnswer(prompt, userId, answer, prevId, null);

        String topicId = topicRepository.findTopicIdByQuestionId(saved.getQuestionId()).orElse(null);

        // NodeUtilService를 통해 맵 반환 (DTO 변환 포함)
        return nodeUtilService.buildMapFromFlatList(List.of(QuestionAnswerDTO.from(saved, Collections.emptyList())), topicId, false);
    }
}
