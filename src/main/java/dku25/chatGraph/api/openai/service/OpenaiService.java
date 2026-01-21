package dku25.chatGraph.api.openai.service;

import dku25.chatGraph.api.exception.ResourceNotFoundException;
import dku25.chatGraph.api.graph.dto.QuestionAnswerDTO;
import dku25.chatGraph.api.graph.dto.TopicTreeMapResponseDTO;
import dku25.chatGraph.api.graph.node.QuestionNode;
import dku25.chatGraph.api.graph.repository.QuestionRepository;
import dku25.chatGraph.api.graph.repository.TopicRepository;
import dku25.chatGraph.api.graph.service.GraphService;
import dku25.chatGraph.api.graph.service.NodeUtilService;
import dku25.chatGraph.api.graph.service.TopicService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OpenaiService {

    // 외부 API 관련
    private final ChatClientService chatClient;
    private final PromptManager promptManager;

    // 그래프 데이터 관련
    private final GraphService graphService;
    private final TopicService topicService;
    private final NodeUtilService nodeUtilService;

    // DB 조회용
    private final QuestionRepository questionRepository;
    private final TopicRepository topicRepository;

    /**
     * AI에게 질문하고 결과를 그래프에 저장 후 반환
     */
    @Transactional // 트랜잭션 묶음 (조회-AI요청-저장-재조회 과정의 데이터 무결성 보장)
    public TopicTreeMapResponseDTO askWithContext(String userId, String prompt, String prevId) {
        // 1. 문맥(Context) 조회
        List<QuestionNode> contextChain = (prevId == null) ? Collections.emptyList() : questionRepository.findContextChain(prevId);

        // 2. AI 요청 (Prompt 생성 -> 요청 -> 응답)
        String aiAnswer = chatClient.ask(promptManager.createMessages(contextChain, prompt));

        // 3. 결과 저장 및 DTO 반환 분기 처리
        if (prevId == null) {
            return processFirstQuestion(userId, prompt, aiAnswer);
        } else {
            return processFollowUpQuestion(userId, prompt, aiAnswer, prevId);
        }
    }

    // --- Private Helper Methods ---

    // 1. 첫 질문 처리 (새 토픽 생성 + 요약)
    private TopicTreeMapResponseDTO processFirstQuestion(String userId, String prompt, String answer) {
        // 요약 생성
        String summary = chatClient.ask(promptManager.createSummaryMessages(prompt));

        // 저장 (GraphService가 저장 후 노드 반환)
        QuestionNode savedNode = graphService.saveQuestionAndAnswer(prompt, userId, answer, null, summary);

        // 토픽 ID 조회 (저장된 노드가 속한 토픽 찾기)
        String topicId = getTopicIdOrThrow(savedNode.getQuestionId());

        // 전체 트리 맵 반환
        return topicService.getTopicQuestionsMap(topicId, userId);
    }

    // 2. 꼬리 질문 처리 (기존 토픽에 연결)
    private TopicTreeMapResponseDTO processFollowUpQuestion(String userId, String prompt, String answer, String prevId) {
        // 저장
        QuestionNode savedNode = graphService.saveQuestionAndAnswer(prompt, userId, answer, prevId, null);

        // 토픽 ID 조회
        String topicId = getTopicIdOrThrow(savedNode.getQuestionId());

        // 현재 추가된 질문만 DTO로 변환하여 반환
        return nodeUtilService.buildMapFromFlatList(List.of(QuestionAnswerDTO.from(savedNode, Collections.emptyList())), topicId, false);
    }

    // 토픽 ID 조회 헬퍼 (중복 로직 제거)
    private String getTopicIdOrThrow(String questionId) {
        return topicRepository.findTopicIdByQuestionId(questionId).orElseThrow(() -> new ResourceNotFoundException("해당 질문에 연결된 토픽을 찾을 수 없습니다. QuestionId: " + questionId));
    }
}