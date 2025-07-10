package dku25.chatGraph.api.config;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAiConfig {
    @Bean
    public OpenAIClient openAIClient(@Value("${openai.api.key}") String openaiApiKey) {
        return OpenAIOkHttpClient.builder()
                .apiKey(openaiApiKey)
                .build();
    }
}
