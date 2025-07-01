package dku25.chatGraph.api.openai.model;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter @Setter
public class ChatCompletionRequest {
    private String model;
    private List<Message> messages;
}
