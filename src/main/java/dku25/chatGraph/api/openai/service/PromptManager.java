package dku25.chatGraph.api.openai.service;

import com.openai.models.chat.completions.ChatCompletionAssistantMessageParam;
import com.openai.models.chat.completions.ChatCompletionMessageParam;
import com.openai.models.chat.completions.ChatCompletionUserMessageParam;
import dku25.chatGraph.api.graph.node.QuestionNode;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Stream;

@Component
public class PromptManager {
    // 1. 체인과 현재 질문을 합쳐 최종 메시지 리스트 생성
    public List<ChatCompletionMessageParam> createMessages(List<QuestionNode> chain, String currentPrompt) {
        return Stream.concat(
                chain.stream().flatMap(this::nodeToMessageStream), // 과거 대화
                Stream.of(userMsg(currentPrompt))                  // 현재 질문
        ).toList();
    }

    // 2. 노드 하나를 [질문, 답변] 메시지로 변환 (질문 있으면 답변은 무조건 있다는 규칙)
    private Stream<ChatCompletionMessageParam> nodeToMessageStream(QuestionNode node) {
        return Stream.of(
                userMsg(node.getText()),
                assistantMsg(node.getAnswer().getText())
        );
    }

    // 3. 요약 요청용 메시지 생성
    public List<ChatCompletionMessageParam> createSummaryMessages(String prompt) {
        return List.of(userMsg(prompt + "\n(이 질문을 토픽 형태로 20자 이내로 요약해줘. 마침표 없이)"));
    }

    // --- Helper Methods ---
    private ChatCompletionMessageParam userMsg(String content) {
        return ChatCompletionMessageParam.ofUser(ChatCompletionUserMessageParam.builder().content(content).build());
    }

    private ChatCompletionMessageParam assistantMsg(String content) {
        return ChatCompletionMessageParam.ofAssistant(ChatCompletionAssistantMessageParam.builder().content(content).build());
    }
}
