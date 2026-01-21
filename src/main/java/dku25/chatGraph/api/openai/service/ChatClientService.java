package dku25.chatGraph.api.openai.service;

import com.openai.client.OpenAIClient;
import com.openai.models.ChatModel;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.chat.completions.ChatCompletionMessageParam;
import dku25.chatGraph.api.exception.ExternalApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatClientService {
    private final OpenAIClient openaiClient;

    @Value("${openai.model.default}")
    private String modelName;

    public String ask(List<ChatCompletionMessageParam> messages) {
        try {
            ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                    .model(ChatModel.of(modelName)) // 또는 문자열 직접 사용
                    .messages(messages)
                    .build();

            return openaiClient.chat().completions().create(params)
                    .choices().get(0).message().content()
                    .orElseThrow(() -> new ExternalApiException("AI 응답이 없습니다."));
        } catch (Exception e) {
            throw new ExternalApiException("OpenAI 통신 오류", e);
        }
    }
}
